package top.egon.enjoyshop.goods.application.goods;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
@Builder
public class FileUploadCommand {

    @NotNull
    @Size(min = 1, max = 10 * 1024 * 1024)
    @ToString.Exclude
    private byte[] content;

    public byte[] getContent() {
        return copy(content);
    }

    public FileUploadCommand setContent(byte[] content) {
        this.content = copy(content);
        return this;
    }

    private static byte[] copy(byte[] source) {
        return source == null ? null : source.clone();
    }

    public static class FileUploadCommandBuilder {

        public FileUploadCommandBuilder content(byte[] content) {
            this.content = copy(content);
            return this;
        }
    }
}
