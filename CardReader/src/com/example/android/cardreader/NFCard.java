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

import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public final class NFCard extends Activity {
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private Resources res;
	private TextView board;

	private enum ContentType {
		HINT, DATA, MSG
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.cardreader);

		final Resources res = getResources();
		this.res = res;

		final View decor = getWindow().getDecorView();
		final TextView board = (TextView) decor.findViewById(R.id.board);
		this.board = board;

		board.setMovementMethod(LinkMovementMethod.getInstance());
		board.setFocusable(false);
		board.setClickable(false);
		board.setLongClickable(false);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		onNewIntent(getIntent());
	}


	@Override
	protected void onPause() {
		super.onPause();

		if (nfcAdapter != null)
			nfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (nfcAdapter != null)
			nfcAdapter.enableForegroundDispatch(this, pendingIntent,
					CardReader.FILTERS, CardReader.TECHLISTS);

		Log.e("NFC----", IsoDep.class.getName());
		refreshStatus();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Parcelable p = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		Log.d("NFCTAG", intent.getAction());
		showData((p != null) ? CardReader.load(p, res) : null);
	}


	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		refreshStatus();
	}

	private void refreshStatus() {
		final Resources r = this.res;

		final String tip;
		if (nfcAdapter == null)
			tip = r.getString(R.string.tip_nfc_notfound);
		else if (nfcAdapter.isEnabled())
			tip = r.getString(R.string.tip_nfc_enabled);
		else
			tip = r.getString(R.string.tip_nfc_disabled);

		final StringBuilder s = new StringBuilder(
				r.getString(R.string.app_name));

		s.append("  --  ").append(tip);
		setTitle(s);

		final CharSequence text = board.getText();

	}

	private void showData(String data) {
		if (data == null || data.length() == 0) {

			return;
		}

		final TextView board = this.board;
		final Resources res = this.res;

		final int padding = res.getDimensionPixelSize(R.dimen.pnl_margin);

		board.setPadding(padding, padding, padding, padding);
		board.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
		board.setTextSize(res.getDimension(R.dimen.text_small));
		board.setTextColor(res.getColor(R.color.text_default));
		board.setGravity(Gravity.NO_GRAVITY);
		board.setTag(ContentType.DATA);
		board.setText(Html.fromHtml(data));
	}


}
