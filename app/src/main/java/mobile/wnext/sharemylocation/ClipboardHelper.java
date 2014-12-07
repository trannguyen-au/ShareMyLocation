package mobile.wnext.sharemylocation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Wery7 on 6/12/2014.
 */
public class ClipboardHelper {

    final Context mContext;

    public ClipboardHelper(Context context) {
        mContext = context;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public boolean copyToClipboard(String text) {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mContext
                        .getSystemService(mContext.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) mContext
                        .getSystemService(mContext.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(
                                mContext.getResources().getString(
                                        R.string.message), text);
                clipboard.setPrimaryClip(clip);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("NewApi")
    public String readFromClipboard() {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) mContext
                    .getSystemService(mContext.CLIPBOARD_SERVICE);
            return clipboard.getText().toString();
        } else {
            ClipboardManager clipboard = (ClipboardManager) mContext
                    .getSystemService(Context.CLIPBOARD_SERVICE);

            // Gets a content resolver instance
            ContentResolver cr = mContext.getContentResolver();

            // Gets the clipboard data from the clipboard
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount()>0) {

                String text = null;

                // Gets the first item from the clipboard data
                ClipData.Item item = clip.getItemAt(0);

                // Tries to get the item's contents as a URI pointing to a note
                Uri uri = item.getUri();

                // If the contents of the clipboard wasn't a reference to a
                // note, then
                // this converts whatever it is to text.
                if (text == null) {
                    text = coerceToText(item).toString();
                }

                return text;
            }
        }
        return "";
    }

    @SuppressLint("NewApi")
    public CharSequence coerceToText(ClipData.Item item) {
        // If this Item has an explicit textual value, simply return that.
        CharSequence text = item.getText();
        if (text != null) {
            return text;
        }

        // If this Item has a URI value, try using that.
        Uri uri = item.getUri();
        if (uri != null) {

            // First see if the URI can be opened as a plain text stream
            // (of any sub-type). If so, this is the best textual
            // representation for it.
            FileInputStream stream = null;
            try {
                // Ask for a stream of the desired type.
                AssetFileDescriptor descr = mContext.getContentResolver()
                        .openTypedAssetFileDescriptor(uri, "text/*", null);
                stream = descr.createInputStream();
                InputStreamReader reader = new InputStreamReader(stream,
                        "UTF-8");

                // Got it... copy the stream into a local string and return it.
                StringBuilder builder = new StringBuilder(128);
                char[] buffer = new char[8192];
                int len;
                while ((len = reader.read(buffer)) > 0) {
                    builder.append(buffer, 0, len);
                }
                return builder.toString();

            } catch (FileNotFoundException e) {
                // Unable to open content URI as text... not really an
                // error, just something to ignore.

            } catch (IOException e) {
                // Something bad has happened.
                Log.w("ClippedData", "Failure loading text", e);
                return e.toString();

            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }

            // If we couldn't open the URI as a stream, then the URI itself
            // probably serves fairly well as a textual representation.
            return uri.toString();
        }

        // Finally, if all we have is an Intent, then we can just turn that
        // into text. Not the most user-friendly thing, but it's something.
        Intent intent = item.getIntent();
        if (intent != null) {
            return intent.toUri(Intent.URI_INTENT_SCHEME);
        }

        // Shouldn't get here, but just in case...
        return "";
    }
}
