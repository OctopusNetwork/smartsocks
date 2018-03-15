package io.smarttangle.blockchain.Utils;

import android.util.Base64;

/**
 * 用于统一Base64编码
 * <p/>
 *
 * @see Base64#NO_WRAP
 * @see Base64#URL_SAFE
 * <p/>
 * Created by yangcheng on 14/12/11.
 */
public class Base64Utils {

    public static final int FLAG = Base64.NO_WRAP | Base64.URL_SAFE;

    public static byte[] encode(byte[] input) {
        return Base64.encode(input, FLAG);
    }

    public static String encodeToString(byte[] input) {
        return Base64.encodeToString(input, FLAG);
    }

    public static byte[] decode(byte[] input) {
        return Base64.decode(input, FLAG);
    }
}
