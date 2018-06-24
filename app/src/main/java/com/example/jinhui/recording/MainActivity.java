package com.example.jinhui.recording;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Chronometer.OnChronometerTickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity {
	private Button mIv_ly;
	private Chronometer mChronometer;
	private TextView mTv_lyzt;
	private Button mIv_cancle;
	private Button mIv_save;
	private ListView mLv_recordList;
	public static final String ROOTPATH = getRootPath();
	// 语音文件
	private String fileName = null;
	// 音频文件保存的路径
	private static String path = "";
	private MediaRecorder mRecorder = null;// 录音器
	private boolean isPause = true;// 当前录音是否处于暂停状态即非录音状态
	private boolean isPausePlay = false;// 当前播放器是否处于暂停状态
	private ArrayList<String> mLypdList = new ArrayList<String>();// 待合成的录音片段
	private String deleteStr = null; // 列表中要删除的文件名
	private long limitTime = 1000;// 录音文件最短事件1秒
	private long currentTime;
	private ArrayList<String> mLylist = new ArrayList<String>();// 录音文件列表
	private ArrayAdapter<String> mLyAdapter;
	// 语音操作对象
	private MediaPlayer mPlayer = null;// 播放器
	private String hcfileName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		findViews();
		initData();
		initList();
	}

	private void initData() {
		path = getRootPath() + "/Record";
		createFile();// 创造文件路径
	}

	public static String getRootPath() {
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			return Environment.getExternalStorageDirectory().getPath()
					+ "/yxpb";
		} else {
			return "/sdcard" + "/yxpb";
		}
	}

	// 初始化录音列表
	private void initList() {
		// 判断SD卡是否存在
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "SD卡状态异常，无法获取录音列表！", Toast.LENGTH_LONG).show();
		} else {
			// 根据后缀名进行判断、获取文件夹中的音频文件
			File file = new File(path);
			File files[] = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					if (files[i].getName().indexOf(".") >= 0) {
						// 只取.amr .mp3
						// .mp4 文件
						String fileStr = files[i].getName().substring(
								files[i].getName().indexOf("."));
						if (fileStr.toLowerCase().equals(".mp3")
								|| fileStr.toLowerCase().equals(".amr")
								|| fileStr.toLowerCase().equals(".mp4"))
							mLylist.add(files[i].getName());
					}
				}
				mLyAdapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, mLylist);
				mLv_recordList.setAdapter(mLyAdapter);
				mLv_recordList
						.setOnItemClickListener(new OnItemClickListener() {

							@Override
							public void onItemClick(AdapterView<?> parent,
													View view, int position, long id) {
								playRecord(path + "/" + mLylist.get(position));
							}
						});
			}

		}
	}

	public void playRecord(String playFileName) {
		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
		mPlayer = new MediaPlayer();
		// 播放完毕的监听
		mPlayer.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer mp) {
				// 播放完毕改变状态，释放资源
				mPlayer.release();
				mPlayer = null;
			}
		});
		try {
			// 播放所选中的录音
			mPlayer.setDataSource(playFileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (Exception e) {
			// 若出现异常被捕获后，同样要释放掉资源
			// 否则程序会不稳定，不适合正式项目上使用
			e.printStackTrace();
			if (mPlayer != null) {
				mPlayer.release();
				mPlayer = null;
			}
			Toast.makeText(this, "播放失败,可返回重试！", Toast.LENGTH_LONG).show();
		}
	}

	// 创建文件夹
	public static boolean createFile() {
		try {
			File file = new File(ROOTPATH);
			if (!file.exists()) {
				file.mkdir();
			}
			file = new File(path);
			if (!file.exists()) {
				file.mkdir();
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void doClick(View v) {
		switch (v.getId()) {
			case R.id.iv_ly:
				startRecord();
				break;
			case R.id.iv_cancle:
				cancleRecord();
				break;
			case R.id.iv_save:
				saveRecord();
				break;
		}

	}

	private void findViews() {
		mIv_ly = (Button) findViewById(R.id.iv_ly);
		mChronometer = (Chronometer) findViewById(R.id.chronometer);
		mTv_lyzt = (TextView) findViewById(R.id.tv_lyzt);
		mIv_cancle = (Button) findViewById(R.id.iv_cancle);
		mIv_save = (Button) findViewById(R.id.iv_save);
		mLv_recordList = (ListView) findViewById(R.id.lv_recordList);
		mIv_cancle.setEnabled(false);
		mIv_save.setEnabled(false);
		mChronometer.setOnChronometerTickListener(new OnChronometerTickListener() {

			@Override
			public void onChronometerTick(Chronometer chronometer) {
				//录音时长的监听器
				if (SystemClock.elapsedRealtime() - mChronometer.getBase() > 30*60 * 1000)
				{
					logMsg("录音超时");
					//todo
				}
			}
		});
	}

	public void cancleRecord() {
		onRecordStop();
		mTv_lyzt.setText("");
		mIv_cancle.setEnabled(false);
		mIv_save.setEnabled(false);
		mIv_ly.setSelected(false);
		if (mRecorder != null) {
			mRecorder.stop();// 停止录音
			mRecorder.release();// 释放录音机资源
			mRecorder = null;

		}
		if (isPause) {// 正在暂停期间
			// 删除已经有的录音片段
		} else {// 录音状态
			logMsg("取消--》");
			if (System.currentTimeMillis() - currentTime < limitTime) {
				// 录音少于一秒不保存
				logMsg("录音少于一秒");
				File pdFile = new File(fileName);
				if (fileName != null && pdFile.exists()) {
					pdFile.delete();
				}

			} else {
				if (fileName != null && mLypdList != null) {
					mLypdList.add(fileName);
				}
				logMsg("保存片段:---->" + fileName);
			}
		}

		for (int i = 0; i < mLypdList.size(); i++) {
			File file = new File(mLypdList.get(i));
			if (file.exists()) {
				file.delete();
			}
		}
		mLypdList.clear();// 清空录音片段
		isPause=true;

	}
	private long recordingTime = 0;// 记录下来的总时间

	public void onRecordStart() {
		mChronometer.setBase(SystemClock.elapsedRealtime() - recordingTime);// 跳过已经记录了的时间，起到继续计时的作用
		mChronometer.start();
	}

	public void onRecordPause() {
		mChronometer.stop();
		recordingTime = SystemClock.elapsedRealtime() - mChronometer.getBase();// 保存这次记录了的时间
	}

	public void onRecordStop() {
		recordingTime = 0;
		mChronometer.setBase(SystemClock.elapsedRealtime());
		mChronometer.stop();
	}

	public void saveRecord() {
		onRecordStop();
		mIv_cancle.setEnabled(false);
		mIv_save.setEnabled(false);
		mIv_ly.setSelected(false);
		mIv_ly.setEnabled(false);
		mTv_lyzt.setText("");
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
		if (isPause) {// 正在暂停期间
			// 保存已经有的录音片段
		} else {// 录音状态
			logMsg("保存--》");
			if (System.currentTimeMillis() - currentTime < limitTime) {
				// 录音少于一秒不保存
				logMsg("录音少于一秒");
				File pdFile = new File(fileName);
				if (fileName != null && pdFile.exists()) {
					pdFile.delete();
				}

			} else {
				if (fileName != null && mLypdList != null) {
					mLypdList.add(fileName);
				}
				logMsg("保存片段:---->" + fileName);
			}
		}
		// 最后合成的音频文件
		hcfileName = path + "/" + getTime() + ".amr";
		String fileName1 = getTime() + ".amr";
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(hcfileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		FileInputStream fileInputStream = null;
		try {
			logMsg("录音片段的长度---->" + mLypdList.size());
			for (int i = 0; i < mLypdList.size(); i++) {
				File file = new File(mLypdList.get(i));
				// 把因为暂停所录出的多段录音进行读取
				fileInputStream = new FileInputStream(file);
				byte[] mByte = new byte[fileInputStream.available()];
				int length = mByte.length;
				// 第一个录音文件的前六位是不需要删除的
				if (i == 0) {
					while (fileInputStream.read(mByte) != -1) {
						fileOutputStream.write(mByte, 0, length);
					}
				}
				// 之后的文件，去掉前六位
				else {
					while (fileInputStream.read(mByte) != -1) {

						fileOutputStream.write(mByte, 6, length - 6);
					}
				}
			}
			if (mLypdList.size() > 0) {
				mLylist.add(fileName1);
				mLyAdapter.notifyDataSetChanged();
				logMsg("合成成功");
			}
		} catch (Exception e) {
			// 这里捕获流的IO异常，万一系统错误需要提示用户
			e.printStackTrace();
			Toast.makeText(this, "录音合成出错，请重试！", Toast.LENGTH_LONG).show();// 出错后删除片段
			for (int i = 0; i < mLypdList.size(); i++) {
				File file = new File(mLypdList.get(i));
				if (file.exists()) {
					file.delete();
				}
			}
		} finally {
			try {
				if (fileOutputStream != null) {
					fileOutputStream.flush();
				}

				if (fileInputStream != null) {
					fileInputStream.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			// 录音结束 、时间归零
		}
		// 不管合成是否成功、删除录音片段
		for (int i = 0; i < mLypdList.size(); i++) {
			File file = new File(mLypdList.get(i));
			if (file.exists()) {
				file.delete();
			}
		}
		mLypdList.clear();
		mIv_ly.setEnabled(true);

	}

	public void startRecord() {
		// 判断SD卡是否存在
		if (!Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			Toast.makeText(this, "SD卡状态异常，请检查后重试！", Toast.LENGTH_LONG).show();
			return;
		}
		mIv_cancle.setEnabled(true);
		mIv_save.setEnabled(true);
		if (isPause) {
			isPause = false;
			currentTime = System.currentTimeMillis();
			fileName = path + "/" + getTime() + ".amr";
			mTv_lyzt.setText("正在录音");
			onRecordStart();
			mIv_ly.setSelected(false);
			mRecorder = new MediaRecorder();
			mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			// 选择amr格式
			mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
			mRecorder.setOutputFile(fileName);
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			try {
				mRecorder.prepare();

			} catch (Exception e) {
				// 若录音器启动失败就需要重启应用，屏蔽掉按钮的点击事件。 否则会出现各种异常。
				Toast.makeText(this, "录音器启动失败，请返回重试！", Toast.LENGTH_LONG)
						.show();
				mRecorder.release();
				mRecorder = null;
			}
			if (mRecorder != null) {
				mRecorder.start();
			}
		} else {
			isPause = true;
			mTv_lyzt.setText("暂停录音");
			onRecordPause();
			mIv_ly.setSelected(true);
			if (mRecorder != null) {
				mRecorder.stop();
				mRecorder.release();
				mRecorder = null;
				// 将录音片段加入列表
				if (System.currentTimeMillis() - currentTime < limitTime) {
					// 录音少于一秒不保存
					logMsg("录音少于一秒");
					File pdFile = new File(fileName);
					if (fileName != null && pdFile.exists()) {
						pdFile.delete();
					}

				} else {
					if (fileName != null && mLypdList != null) {
						mLypdList.add(fileName);
					}
					logMsg("片段:---->" + fileName);
				}

			}
		}

	}

	private void logMsg(String msg) {
		LogUtil.i("TAG", msg);
	}

	// 获得当前时间
	private String getTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHH-mm-ss");
		Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
		String time = formatter.format(curDate);
		return time;
	}

}

