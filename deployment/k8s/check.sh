#!/usr/bin/env bash
# Offline release-input contract checks; never renders or deploys resources.
set -eu
if ! command -v python3 >/dev/null 2>&1; then
    printf '%s\n' 'PREREQUISITE_MISSING: python3' >&2
    exit 2
fi
exec python3 - "${BASH_SOURCE[0]}" "$@" <<'PY'
import copy
import hashlib
import json
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from urllib.parse import urlsplit

SCRIPT = Path(sys.argv[1]).resolve()
ROOT = SCRIPT.parents[2]
DIRECTORY = ROOT / 'deployment' / 'k8s'
SCHEMA_PATH = DIRECTORY / 'release-inputs.schema.json'
EXAMPLE_PATH = DIRECTORY / 'release-inputs.example.json'
CORE_FIELDS = (
    'clusterVersion', 'gatewayApiVersion', 'envoyGatewayVersion', 'envoyProxyImageDigest',
    'appNamespace', 'platformNamespace', 'envoyNamespace', 'gatewayClassName',
    'publicHostname', 'publicOrigin', 'issuerUri', 'oidcCallbackUri',
    'tlsSecretName', 'edgeCaReference', 'edgeTlsServerName',
    'edgeServiceName', 'edgeServicePort', 'staticServicePort', 'trustedProxyChain',
    'clientIpMode', 'staticImageDigest', 'staticArtifactVersion', 'assetRetentionPolicy',
    'envoyReplicas', 'staticMinReplicas', 'staticMaxReplicas', 'resourceRequests',
    'resourceLimits', 'ingressLocalLimit', 'burst', 'bizPolicyRefs', 'quotaFailureMode',
    'readyProbeContract', 'drainTimeout', 'terminationGracePeriod', 'requestTimeouts',
    'redisHaEvidence', 'dbCapacityEvidence', 'platformAdmissionEvidence',
)
CREDENTIAL_KEYS = {'password', 'token', 'privatekey', 'clientsecret', 'secretkey', 'accesskey'}
VERSION_FIELDS = ('clusterVersion', 'gatewayApiVersion', 'envoyGatewayVersion')


class InputError(ValueError):
    """Only fixed codes and trusted field paths are exposed to the caller."""
    def __init__(self, code, field='$'):
        self.code = code
        self.field = field
        super().__init__(f'{code}: {field}')


def load_json(path, missing_code='INPUT_UNREADABLE'):
    def unique_object(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise InputError('DUPLICATE_KEY')
            result[key] = value
        return result

    def reject_constant(_value):
        raise InputError('INVALID_JSON')

    try:
        with Path(path).open(encoding='utf-8') as source:
            return json.load(source, object_pairs_hook=unique_object, parse_constant=reject_constant)
    except InputError:
        raise
    except OSError:
        raise InputError(missing_code) from None
    except (ValueError, UnicodeError, RecursionError):
        raise InputError('INVALID_JSON') from None


def reject_credentials(value, field='$'):
    if isinstance(value, dict):
        for key, child in value.items():
            normalized = re.sub(r'[^a-z0-9]', '', key.lower())
            if normalized in CREDENTIAL_KEYS:
                raise InputError('CREDENTIAL_FIELD', field)
            # Opaque environment record names are untrusted and are not logged.
            reject_credentials(child, field)
    elif isinstance(value, list):
        for child in value:
            reject_credentials(child, field)
    elif isinstance(value, str) and re.search(r'-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----', value):
        raise InputError('CREDENTIAL_VALUE', field)


def https_uri(value, field):
    try:
        parsed = urlsplit(value)
        if (parsed.scheme != 'https' or not parsed.hostname or parsed.username is not None
                or parsed.password is not None or parsed.fragment or any(c.isspace() for c in value)):
            raise InputError('HTTPS_URI_REQUIRED', field)
        port = 443 if parsed.port is None else parsed.port
        if not 1 <= port <= 65535:
            raise InputError('HTTPS_URI_REQUIRED', field)
        return parsed, (parsed.hostname.lower(), port)
    except ValueError:
        raise InputError('HTTPS_URI_REQUIRED', field) from None


def validate_release_inputs(schema, inputs):
    # This is the dedicated 39-field contract checker, not a JSON Schema engine.
    if (schema.get('$schema') != 'https://json-schema.org/draft/2020-12/schema'
            or schema.get('type') != 'object' or schema.get('additionalProperties') is not False
            or set(schema.get('required', [])) != set(CORE_FIELDS)
            or set(schema.get('properties', {})) != set(CORE_FIELDS)):
        raise InputError('SCHEMA_CONTRACT_MISMATCH')
    if not isinstance(inputs, dict):
        raise InputError('OBJECT_REQUIRED')
    reject_credentials(inputs)
    if set(inputs) - set(CORE_FIELDS):
        raise InputError('UNKNOWN_FIELD')
    for name in CORE_FIELDS:
        field = '$.' + name
        if name not in inputs or inputs[name] is None:
            raise InputError('INPUT_REQUIRED', field)
        value, rule = inputs[name], schema['properties'][name]
        kind = rule['type']
        valid_type = {'string': isinstance(value, str), 'integer': type(value) is int,
                      'object': isinstance(value, dict), 'array': isinstance(value, list)}.get(kind)
        if not valid_type:
            raise InputError('TYPE_MISMATCH', field)
        if kind == 'string':
            if not value.strip() or len(value) < rule.get('minLength', 0):
                raise InputError('NONEMPTY_REQUIRED', field)
            if 'maxLength' in rule and len(value) > rule['maxLength']:
                raise InputError('OUT_OF_RANGE', field)
            if 'pattern' in rule and not re.fullmatch(rule['pattern'], value):
                code = 'HTTPS_URI_REQUIRED' if name in ('publicOrigin', 'issuerUri', 'oidcCallbackUri') else 'PATTERN_MISMATCH'
                raise InputError(code, field)
        if kind == 'integer':
            if value < rule.get('minimum', value) or value > rule.get('maximum', value):
                raise InputError('OUT_OF_RANGE', field)
        if kind in ('object', 'array') and not value:
            raise InputError('NONEMPTY_REQUIRED', field)
        if 'const' in rule and value != rule['const']:
            raise InputError('VALUE_NOT_ALLOWED', field)
        if name in VERSION_FIELDS and 'latest' in value.lower():
            raise InputError('PINNED_VERSION_REQUIRED', field)
        if name == 'bizPolicyRefs' and any(not isinstance(v, str) or not v.strip() for v in value):
            raise InputError('POLICY_REFERENCE_REQUIRED', field)
        if name == 'trustedProxyChain' and any(not isinstance(v, dict) or not v for v in value):
            raise InputError('PROXY_RECORD_REQUIRED', field)
    if inputs['staticMaxReplicas'] < inputs['staticMinReplicas']:
        raise InputError('MAX_BELOW_MIN', '$.staticMaxReplicas')
    timeouts = inputs['requestTimeouts']
    if (type(timeouts.get('inFlightSeconds')) is not int or timeouts['inFlightSeconds'] <= 0):
        raise InputError('POSITIVE_SECONDS_REQUIRED', '$.requestTimeouts.inFlightSeconds')
    if inputs['terminationGracePeriod'] <= inputs['drainTimeout'] + timeouts['inFlightSeconds']:
        raise InputError('GRACE_BUDGET_TOO_SHORT', '$.terminationGracePeriod')
    origin, origin_key = https_uri(inputs['publicOrigin'], '$.publicOrigin')
    callback, callback_key = https_uri(inputs['oidcCallbackUri'], '$.oidcCallbackUri')
    https_uri(inputs['issuerUri'], '$.issuerUri')
    if origin.path not in ('', '/') or origin.query:
        raise InputError('ORIGIN_NOT_ROOT', '$.publicOrigin')
    if origin.hostname.lower() != inputs['publicHostname'].lower():
        raise InputError('ORIGIN_HOST_MISMATCH', '$.publicOrigin')
    if callback_key != origin_key:
        raise InputError('CALLBACK_ORIGIN_MISMATCH', '$.oidcCallbackUri')
    if 'latest' in inputs['staticArtifactVersion'].lower():
        raise InputError('PINNED_VERSION_REQUIRED', '$.staticArtifactVersion')


def protected_snapshot():
    paths = [ROOT / 'pom.xml', ROOT / 'enjoyshop-web' / 'pom.xml']
    service = ROOT / 'enjoyshop-service'
    if service.exists():
        paths.extend(p for p in service.rglob('*') if p.is_file() and 'target' not in p.parts
                     and (p.suffix in ('.java', '.sql', '.yml', '.yaml')
                          or p.name in ('pom.xml', 'repository-manifest.json', 'lombok.config')))
    return {str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in paths if p.is_file()}


def check_scope(before):
    if protected_snapshot() != before:
        raise InputError('SOURCE_SCOPE_CHANGED')


def synthetic_inputs():
    # Ephemeral unit-test data only; never exported as a release example.
    values = {name: 'synthetic-reference' for name in CORE_FIELDS}
    values.update({name: 'v1.0.0-synthetic' for name in VERSION_FIELDS})
    values.update({name: 'synthetic' for name in (
        'appNamespace', 'platformNamespace', 'envoyNamespace', 'gatewayClassName',
        'tlsSecretName', 'edgeCaReference', 'edgeServiceName')})
    values.update(publicHostname='synthetic.invalid', publicOrigin='https://synthetic.invalid',
                  issuerUri='https://issuer.synthetic.invalid',
                  oidcCallbackUri='https://synthetic.invalid/login/oauth2/code/edge-ui',
                  edgeTlsServerName='edge.synthetic.invalid', edgeServicePort=8443, staticServicePort=8080,
                  envoyProxyImageDigest='synthetic/envoy@sha256:' + '0' * 64,
                  staticImageDigest='synthetic/static@sha256:' + '1' * 64,
                  envoyReplicas=2, staticMinReplicas=2, staticMaxReplicas=4,
                  ingressLocalLimit=100, burst=0, quotaFailureMode='DENY', clientIpMode='synthetic-mode',
                  trustedProxyChain=[{'owner': 'synthetic'}], bizPolicyRefs=['synthetic-policy'],
                  drainTimeout=5, terminationGracePeriod=30, requestTimeouts={'inFlightSeconds': 20})
    for name in ('assetRetentionPolicy', 'resourceRequests', 'resourceLimits', 'readyProbeContract'):
        values[name] = {'owner': 'synthetic'}
    return values


class ReleaseInputTests(unittest.TestCase):
    def setUp(self):
        self.inputs = synthetic_inputs()
        self.schema = load_json(SCHEMA_PATH, 'SCHEMA_MISSING')

    def rejected(self, field, value, code):
        self.inputs[field] = value
        with self.assertRaises(InputError) as result:
            validate_release_inputs(self.schema, self.inputs)
        self.assertEqual(code, result.exception.code)
        self.assertTrue(result.exception.field == '$' or result.exception.field.startswith('$.' + field))

    def test_synthetic_complete_contract(self):
        self.assertEqual(39, len(self.inputs))
        validate_release_inputs(self.schema, self.inputs)

    def test_missing_schema(self):
        with self.assertRaises(InputError) as result:
            load_json(DIRECTORY / 'missing-schema.json', 'SCHEMA_MISSING')
        self.assertEqual('SCHEMA_MISSING', result.exception.code)

    def test_missing_required(self):
        for field in CORE_FIELDS:
            with self.subTest(field=field):
                candidate = copy.deepcopy(self.inputs)
                del candidate[field]
                with self.assertRaises(InputError) as result:
                    validate_release_inputs(self.schema, candidate)
                self.assertEqual('INPUT_REQUIRED', result.exception.code)

    def test_null(self):
        for field in CORE_FIELDS:
            with self.subTest(field=field):
                candidate = copy.deepcopy(self.inputs)
                candidate[field] = None
                with self.assertRaises(InputError) as result:
                    validate_release_inputs(self.schema, candidate)
                self.assertEqual('INPUT_REQUIRED', result.exception.code)

    def test_unknown_field(self):
        self.inputs['untrusted-field'] = 'do-not-log'
        with self.assertRaises(InputError) as result:
            validate_release_inputs(self.schema, self.inputs)
        self.assertEqual('UNKNOWN_FIELD', result.exception.code)
        self.assertNotIn('do-not-log', str(result.exception))

    def test_wrong_type(self):
        for field, rule in self.schema['properties'].items():
            with self.subTest(field=field):
                bad = False if rule['type'] == 'integer' else 3
                candidate = copy.deepcopy(self.inputs)
                candidate[field] = bad
                with self.assertRaises(InputError) as result:
                    validate_release_inputs(self.schema, candidate)
                self.assertEqual('TYPE_MISMATCH', result.exception.code)

    def test_versions_latest(self):
        for field in VERSION_FIELDS:
            with self.subTest(field=field):
                for value in ('latest', 'LATEST', 'v1-latest', 'notlatest'):
                    self.rejected(field, value, 'PINNED_VERSION_REQUIRED')
                    self.inputs = synthetic_inputs()

    def test_blank_string_and_empty_record(self):
        self.rejected('edgeCaReference', '   ', 'NONEMPTY_REQUIRED')
        self.inputs = synthetic_inputs()
        self.rejected('readyProbeContract', {}, 'NONEMPTY_REQUIRED')

    def test_invalid_namespace_and_hostname(self):
        for field, value in (('appNamespace', 'invalid_name'), ('appNamespace', 'valid\n'),
                             ('publicHostname', 'host..invalid'), ('edgeTlsServerName', '-edge.invalid')):
            with self.subTest(field=field):
                self.rejected(field, value, 'PATTERN_MISMATCH')
                self.inputs = synthetic_inputs()

    def test_invalid_policy_and_proxy_records(self):
        self.rejected('bizPolicyRefs', [None], 'POLICY_REFERENCE_REQUIRED')
        self.inputs = synthetic_inputs()
        self.rejected('trustedProxyChain', ['unverified'], 'PROXY_RECORD_REQUIRED')

    def test_quota_numeric_bounds(self):
        self.rejected('ingressLocalLimit', 0, 'OUT_OF_RANGE')
        self.inputs = synthetic_inputs()
        self.rejected('burst', -1, 'OUT_OF_RANGE')

    def test_static_version_latest(self):
        self.rejected('staticArtifactVersion', 'latest', 'PINNED_VERSION_REQUIRED')

    def test_origin_root_and_port(self):
        self.rejected('publicOrigin', 'https://synthetic.invalid/path', 'ORIGIN_NOT_ROOT')
        for port in ('bad', '0', '65536'):
            with self.subTest(port=port):
                self.inputs = synthetic_inputs()
                self.rejected('publicOrigin', 'https://synthetic.invalid:' + port, 'HTTPS_URI_REQUIRED')

    def test_callback_port_and_default_https_port(self):
        self.rejected('oidcCallbackUri', 'https://synthetic.invalid:8443/callback', 'CALLBACK_ORIGIN_MISMATCH')
        self.inputs = synthetic_inputs()
        self.inputs['publicOrigin'] = 'https://synthetic.invalid:443/'
        validate_release_inputs(self.schema, self.inputs)

    def test_invalid_port(self):
        for field in ('edgeServicePort', 'staticServicePort'):
            for value in (0, 65536):
                with self.subTest(field=field, value=value):
                    self.rejected(field, value, 'OUT_OF_RANGE')
                    self.inputs = synthetic_inputs()

    def test_replica_bounds(self):
        self.rejected('envoyReplicas', 1, 'OUT_OF_RANGE')
        self.inputs = synthetic_inputs()
        self.rejected('staticMinReplicas', 1, 'OUT_OF_RANGE')
        self.inputs = synthetic_inputs()
        self.inputs['staticMinReplicas'] = 5
        self.rejected('staticMaxReplicas', 4, 'MAX_BELOW_MIN')

    def test_bad_digest(self):
        for field in ('staticImageDigest', 'envoyProxyImageDigest'):
            with self.subTest(field=field):
                self.rejected(field, 'synthetic/image:latest', 'PATTERN_MISMATCH')
                self.inputs = synthetic_inputs()

    def test_inline_secret(self):
        for key in ('password', 'token', 'privateKey', 'clientSecret', 'secretKey', 'accessKey', 'private_key'):
            with self.subTest(key=key):
                candidate = copy.deepcopy(self.inputs)
                candidate['readyProbeContract'][key] = 'never-print-this'
                with self.assertRaises(InputError) as result:
                    validate_release_inputs(self.schema, candidate)
                self.assertEqual('CREDENTIAL_FIELD', result.exception.code)
                self.assertNotIn('never-print-this', str(result.exception))

    def test_private_key_value(self):
        self.rejected('edgeCaReference', '-----BEGIN PRIVATE KEY-----', 'CREDENTIAL_VALUE')

    def test_origin_mismatch(self):
        self.rejected('publicOrigin', 'https://other.synthetic.invalid', 'ORIGIN_HOST_MISMATCH')

    def test_callback_origin_mismatch(self):
        self.rejected('oidcCallbackUri', 'https://other.synthetic.invalid/login/oauth2/code/edge-ui',
                      'CALLBACK_ORIGIN_MISMATCH')

    def test_uri_credentials_not_allowed(self):
        self.rejected('publicOrigin', 'https://user:never-print@synthetic.invalid', 'HTTPS_URI_REQUIRED')

    def test_http_not_allowed(self):
        self.rejected('issuerUri', 'http://issuer.synthetic.invalid', 'HTTPS_URI_REQUIRED')

    def test_strict_fallback(self):
        self.rejected('quotaFailureMode', 'LOCALFALLBACK', 'VALUE_NOT_ALLOWED')

    def test_grace_not_covering_drain_and_inflight(self):
        self.rejected('terminationGracePeriod', 25, 'GRACE_BUDGET_TOO_SHORT')

    def test_inflight_positive_integer(self):
        for value in (0, -1, True, '20'):
            with self.subTest(value=value):
                self.rejected('requestTimeouts', {'inFlightSeconds': value}, 'POSITIVE_SECONDS_REQUIRED')
                self.inputs = synthetic_inputs()

    def test_example_cannot_be_release(self):
        example = load_json(EXAMPLE_PATH)
        self.assertEqual(list(CORE_FIELDS), list(example))
        self.assertTrue(all(value is None for value in example.values()))
        with self.assertRaises(InputError) as result:
            validate_release_inputs(self.schema, example)
        self.assertEqual('INPUT_REQUIRED', result.exception.code)

    def test_schema_drift(self):
        self.schema['required'] = self.schema['required'][:-1]
        with self.assertRaises(InputError) as result:
            validate_release_inputs(self.schema, self.inputs)
        self.assertEqual('SCHEMA_CONTRACT_MISMATCH', result.exception.code)

    def test_json_duplicate_and_nonfinite(self):
        with tempfile.TemporaryDirectory(prefix='homepage-input-test-') as temporary:
            path = Path(temporary) / 'input.json'
            for payload, expected in (('{"a":1,"a":2}', 'DUPLICATE_KEY'),
                                      ('{"a":NaN}', 'INVALID_JSON'), ('{broken', 'INVALID_JSON')):
                path.write_text(payload)
                with self.assertRaises(InputError) as result:
                    load_json(path)
                self.assertEqual(expected, result.exception.code)

    def test_cli_rejection_redacts_values_and_has_no_outputs(self):
        with tempfile.TemporaryDirectory(prefix='homepage-input-test-') as temporary:
            path = Path(temporary) / 'input.json'
            self.inputs['readyProbeContract']['token'] = 'never-print-this'
            path.write_text(json.dumps(self.inputs))
            result = subprocess.run(['bash', str(SCRIPT), 'inputs', str(path)],
                                    capture_output=True, text=True, check=False)
            self.assertEqual(1, result.returncode)
            self.assertIn('CREDENTIAL_FIELD', result.stderr)
            self.assertNotIn('never-print-this', result.stdout + result.stderr)
            self.assertEqual([path], list(Path(temporary).iterdir()))

    def test_cli_validates_without_deployment_outputs(self):
        with tempfile.TemporaryDirectory(prefix='homepage-input-test-') as temporary:
            path = Path(temporary) / 'input.json'
            path.write_text(json.dumps(self.inputs))
            result = subprocess.run(['bash', str(SCRIPT), 'inputs', str(path)],
                                    capture_output=True, text=True, check=False)
            self.assertEqual(0, result.returncode, result.stderr)
            self.assertEqual('INPUT_CONTRACT_VALID\n', result.stdout)
            self.assertNotIn('DEPLOY_READY', result.stdout)
            self.assertEqual([path], list(Path(temporary).iterdir()))

    def test_cli_requires_explicit_readable_json_and_supported_mode(self):
        with tempfile.TemporaryDirectory(prefix='homepage-input-test-') as temporary:
            missing = Path(temporary) / 'missing.json'
            malformed = Path(temporary) / 'malformed.json'
            malformed.write_text('{"token": never-print-this}')
            for args, code, message in (([], 2, 'USAGE'), (['deploy'], 2, 'USAGE'),
                                        (['inputs'], 2, 'USAGE'),
                                        (['inputs', str(missing)], 1, 'INPUT_UNREADABLE'),
                                        (['inputs', str(malformed)], 1, 'INVALID_JSON')):
                with self.subTest(args=args[:1]):
                    result = subprocess.run(['bash', str(SCRIPT), *args],
                                            capture_output=True, text=True, check=False)
                    self.assertEqual(code, result.returncode)
                    self.assertIn(message, result.stderr)
                    self.assertNotIn('never-print-this', result.stdout + result.stderr)


def main(args):
    if not args or args[0] not in ('selftest', 'inputs') or len(args) != (1 if args[0] == 'selftest' else 2):
        print('USAGE: check.sh selftest | inputs EXPLICIT_JSON_PATH', file=sys.stderr)
        return 2
    try:
        schema = load_json(SCHEMA_PATH, 'SCHEMA_MISSING')
        if args[0] == 'inputs':
            validate_release_inputs(schema, load_json(args[1]))
            print('INPUT_CONTRACT_VALID')
            return 0
        before = protected_snapshot()
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(ReleaseInputTests)
        result = unittest.TextTestRunner(verbosity=2).run(suite)
        check_scope(before)
        return 0 if result.wasSuccessful() else 1
    except InputError as error:
        print(str(error), file=sys.stderr)
        return 1


sys.exit(main(sys.argv[2:]))
PY
