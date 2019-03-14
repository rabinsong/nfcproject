/* NFCard is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFCard is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package com.example.android.cardreader;

import java.io.IOException;
import java.util.Arrays;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Parcelable;
import android.util.Log;

public final class CardReader {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

	public static String[][] TECHLISTS;
	public static IntentFilter[] FILTERS;

	static {
		try {
			//the tech lists used to perform matching for dispatching of the ACTION_TECH_DISCOVERED intent
			TECHLISTS = new String[][] { { IsoDep.class.getName() },
					{ NfcV.class.getName() }, { NfcF.class.getName() }, };

			FILTERS = new IntentFilter[] { new IntentFilter(
					NfcAdapter.ACTION_TECH_DISCOVERED, "*/*") };
		} catch (Exception e) {
		}
	}

    
	public static String load(Parcelable parcelable, Resources res) {
		//从Parcelable筛选出各类NFC标准数据
		final Tag tag = (Tag) parcelable;
		
		final IsoDep isoDep = IsoDep.get(tag);
		

		Log.e("NFCTAG ID", Util.toHexString(tag.getId(), 0, tag.getId().length));//isodep.transceive("45".getBytes()).toString());

		
		if (isoDep == null) {
			return null;
		}

		
		
        try {
            // Connect to the remote NFC device
            isoDep.connect();
            // Build SELECT AID command for our loyalty card service.
            // This command tells the remote device which service we wish to communicate with.
            Log.e(TAG, "Requesting remote AID: " + SAMPLE_LOYALTY_CARD_AID);
            byte[] command = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID);
            // Send command to remote device
            Log.e(TAG, "Sending: " + ByteArrayToHexString(command));
            byte[] result = isoDep.transceive(command);
            // If AID is successfully selected, 0x9000 is returned as the status word (last 2
            // bytes of the result) by convention. Everything before the status word is
            // optional payload, which is used here to hold the account number.
            int resultLength = result.length;
            byte[] statusWord = {result[resultLength-2], result[resultLength-1]};
            byte[] payload = Arrays.copyOf(result, resultLength-2);
            if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                // The remote NFC device will immediately respond with its stored account number
                String accountNumber = new String(payload, "UTF-8");
                Log.e(TAG, "Received: " + accountNumber);
                return accountNumber;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error communicating with card: " + e.toString());
        }
        
        return null;
	}



    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    
}
