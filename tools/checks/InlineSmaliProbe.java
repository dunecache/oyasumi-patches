import com.android.tools.smali.smali.SmaliTestUtils;

/**
 * Prints PARSE OK when the smali on stdin compiles, otherwise the parser's own diagnostics.
 *
 * This is the same entry point the patcher's InlineSmaliCompiler uses, so a block that passes
 * here will not throw a syntax error at patch time. It does not check semantics: a block can
 * parse and still clobber a live register, so the caller wraps each block in a stub carrying
 * the target method's real register count.
 */
public final class InlineSmaliProbe {
    public static void main(String[] args) {
        String body;
        try {
            body = new String(System.in.readAllBytes(), "UTF-8");
        } catch (Exception e) {
            System.out.println("could not read stdin: " + e);
            return;
        }
        try {
            SmaliTestUtils.compileSmali(body);
            System.out.println("PARSE OK");
        } catch (Throwable t) {
            System.out.println("FAILED: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }
}