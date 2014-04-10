package at.favre.app.blurtest.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import at.favre.app.blurtest.R;
import at.favre.app.blurtest.SettingsController;
import at.favre.app.blurtest.activities.MainActivity;
import at.favre.app.blurtest.util.BlurUtil;

public class StaticBlurFragment extends Fragment implements IFragmentWithBlurSettings {
	private static final String TAG = StaticBlurFragment.class.getSimpleName();

	private ImageView imageViewBlur;
	private ImageView imageViewNormal;

	private Bitmap blurTemplate;
	private SettingsController settingsController;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_staticblur,container,false);

		imageViewNormal= (ImageView) v.findViewById(R.id.normal_image);
		imageViewBlur = (ImageView)  v.findViewById(R.id.blur_image);
		settingsController = new SettingsController(v,new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {reBlur();}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		},new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				blurTemplate = null;
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				reBlur();
			}
		},new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
				reBlur();
			}

			@Override
			public void onNothingSelected(AdapterView<?> adapterView) {

			}
		},new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				blurTemplate = null;
				startBlur();
			}
		},false);

		Bitmap normalBitmap = ((BitmapDrawable)imageViewNormal.getDrawable()).getBitmap();
		((TextView)  v.findViewById(R.id.tv_resolution_normal)).setText("Original: "+normalBitmap.getWidth()+"x"+normalBitmap.getHeight()+" / "+(BlurUtil.sizeOf(normalBitmap)/1024)+"kB");

		return v;
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		startBlur();
	}

	private void startBlur() {
		new BlurTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void reBlur() {
		new BlurTask(true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void switchShowSettings() {
		settingsController.switchShow();
	}


	public class BlurTask extends AsyncTask<Void, Void, Bitmap> {
		private long startWholeProcess;
		private long readBitmapDuration;
		private long blurDuration;

		private boolean onlyReBlur;

		public BlurTask() {
			this(false);
		}

		public BlurTask(boolean onlyReBlur) {
			this.onlyReBlur= onlyReBlur;
		}

		@Override
		protected void onPreExecute() {
			startWholeProcess = SystemClock.elapsedRealtime();
			if(!onlyReBlur) {
				imageViewNormal.setAlpha(1f);
				imageViewBlur.setAlpha(1f);
			}
		}

		@Override
		protected Bitmap doInBackground(Void... voids) {
			if(blurTemplate == null) {
				Log.d(TAG, "Load Bitmap");
				long startReadBitmap = SystemClock.elapsedRealtime();
				final BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = settingsController.getInSampleSize();
				blurTemplate = BitmapFactory.decodeResource(getResources(), R.drawable.photo1, options);
				readBitmapDuration = SystemClock.elapsedRealtime() - startReadBitmap;
			}

			Log.d(TAG,"Start blur algorithm");
			long startBlur = SystemClock.elapsedRealtime();
			Bitmap blurredBitmap=null;

			try {
				blurredBitmap = BlurUtil.blur(((MainActivity)getActivity()).getRs(),blurTemplate, settingsController.getRadius(), settingsController.getAlgorithm());
			} catch (Exception e) {
				Toast.makeText(getActivity(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			}

			blurDuration = SystemClock.elapsedRealtime()- startBlur;
			Log.d(TAG,"Done blur algorithm");
			return  blurredBitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if(bitmap != null) {
				Log.d(TAG, "Set image to imageView");
				imageViewBlur.setImageBitmap(bitmap);
				long duration = (SystemClock.elapsedRealtime() - startWholeProcess);
				Log.d(TAG, "Bluring duration " + duration + "ms");

				if(!onlyReBlur) {
					Toast.makeText(getActivity(), settingsController.getAlgorithm() + " /  insample " + settingsController.getInSampleSize() + " / radius " + settingsController.getRadius() + "px / " + duration + "ms" + " / " + (BlurUtil.sizeOf(bitmap) / 1024) + "kB", Toast.LENGTH_SHORT).show();
				}

				if (settingsController.isShowCrossfade() && !onlyReBlur) {
					final Animation anim = AnimationUtils.loadAnimation(getActivity(), R.animator.alpha_fadeout);
					anim.setFillAfter(true);
					imageViewNormal.startAnimation(anim);
					final Animation anim2 = AnimationUtils.loadAnimation(getActivity(), R.animator.alpha_fadein);
					anim2.setFillAfter(true);
					imageViewBlur.startAnimation(anim2);
				} else {
					imageViewBlur.setAlpha(1.0f);
					imageViewNormal.setAlpha(0.0f);
				}

				Bitmap blurBitmap = ((BitmapDrawable) imageViewBlur.getDrawable()).getBitmap();
				((TextView) getView().findViewById(R.id.tv_resolution_blur)).setText(blurBitmap.getWidth() + "x" + blurBitmap.getHeight() + " / " + (BlurUtil.sizeOf(blurBitmap) / 1024) + "kB / " + settingsController.getAlgorithm() + " / r:" + settingsController.getRadius() + "px / blur: " + blurDuration + "ms / " + duration + "ms");
			}
		}
	}


}