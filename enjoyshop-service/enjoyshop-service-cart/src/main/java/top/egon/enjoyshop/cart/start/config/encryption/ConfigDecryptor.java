package top.egon.enjoyshop.cart.start.config.encryption;

public interface ConfigDecryptor {

    boolean supports(String value);

    String decrypt(String value, char[] key);
}
