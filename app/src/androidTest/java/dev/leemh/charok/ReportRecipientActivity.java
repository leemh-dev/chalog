package dev.leemh.charok;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Separate test-APK UID. Java avoids depending on Kotlin classes supplied only by the target APK. */
public class ReportRecipientActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if ("dev.leemh.charok.test.PROBE_COMMAND".equals(getIntent().getAction())) {
            sendBroadcast(new Intent("CONTROL_SYMPTOM_RECORD").setPackage("dev.leemh.charok")
                .putExtra("ACTION_PARAM", "{\"operation\":\"start\"}"));
            sendBroadcast(new Intent("dev.leemh.charok.TEST_UNTRUSTED_PROBE_FINISHED").setPackage("dev.leemh.charok")
                .putExtra("senderUid", android.os.Process.myUid()));
            finish();
            return;
        }
        Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
        Intent result = new Intent("dev.leemh.charok.TEST_REPORT_RECEIVED").setPackage("dev.leemh.charok");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    if (out.size() + read > 4000000) throw new IllegalArgumentException("Test report exceeds size limit");
                    out.write(buffer, 0, read);
                }
            }
            byte[] data = out.toByteArray();
            StringBuilder digest = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-256").digest(data)) digest.append(String.format("%02x", value));
            result.putExtra("sha256", digest.toString());
            result.putExtra("containsTest", new String(data, StandardCharsets.UTF_8).contains("TEST"));
            String path = uri.getPath();
            Uri sibling = uri.buildUpon().path(path.substring(0, path.lastIndexOf('/')) + "/unselected-scope-test.txt").build();
            boolean denied = false;
            try (InputStream ignored = getContentResolver().openInputStream(sibling)) { }
            catch (SecurityException expected) { denied = true; }
            result.putExtra("siblingDenied", denied);
            result.putExtra("senderUid", android.os.Process.myUid());
        } catch (Exception error) { result.putExtra("readFailed", true); }
        sendBroadcast(result);
        finish();
    }
}
