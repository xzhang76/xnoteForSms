package com.xnote;
import static com.mob.tools.utils.R.getBitmapRes;
import static com.mob.tools.utils.R.getStringRes;
import static com.mob.tools.utils.R.getStyleRes;

import java.util.HashMap;

import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cn.smssdk.EventHandler;
import cn.smssdk.OnSendMessageHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.UserInterruptException;
import cn.smssdk.gui.CommonDialog;
import cn.smssdk.gui.CountryPage;
import cn.smssdk.gui.IdentifyNumPage;
import cn.smssdk.gui.RegisterPage;
import cn.smssdk.gui.SmartVerifyPage;
import cn.smssdk.gui.layout.RegisterPageLayout;
import cn.smssdk.gui.layout.Res;
import cn.smssdk.gui.layout.SendMsgDialogLayout;
import cn.smssdk.utils.SMSLog;


public class MyRegisterPage extends RegisterPage {
	// Ĭ��ʹ���й�����
		private static final String DEFAULT_COUNTRY_ID = "42";

		private EventHandler callback;

		// ����
		private TextView tvCountry;
		// �ֻ�����
		private EditText etPhoneNum;
		// ���ұ��
		private TextView tvCountryNum;
		// clear ����
		private ImageView ivClear;
		// ��һ����ť
		private Button btnNext;

		private String currentId;
		private String currentCode;
		private EventHandler handler;
		private Dialog pd;
		private OnSendMessageHandler osmHandler;

		public void setRegisterCallback(EventHandler callback) {
			this.callback = callback;
		}

		public void setOnSendMessageHandler(OnSendMessageHandler h) {
			osmHandler = h;
		}

		public void show(Context context) {
			super.show(context, null);
		}

		public void onCreate() {

			RegisterPageLayout page = new RegisterPageLayout(activity);
			LinearLayout layout = page.getLayout();

			if (layout != null) {
				activity.setContentView(layout);
				currentId = DEFAULT_COUNTRY_ID;

				View llBack = activity.findViewById(Res.id.ll_back);
				TextView tv = (TextView) activity.findViewById(Res.id.tv_title);
				int resId = getStringRes(activity, "smssdk_regist");
				if (resId > 0) {
					tv.setText(resId);
				}

				View viewCountry = activity.findViewById(Res.id.rl_country);
				btnNext = (Button) activity.findViewById(Res.id.btn_next);
				tvCountry = (TextView) activity.findViewById(Res.id.tv_country);

				String[] country = getCurrentCountry();
				// String[] country = SMSSDK.getCountry(currentId);
				if (country != null) {
					currentCode = country[1];
					tvCountry.setText(country[0]);
				}

				tvCountryNum = (TextView) activity.findViewById(Res.id.tv_country_num);
				tvCountryNum.setText("+" + currentCode);

				etPhoneNum = (EditText) activity.findViewById(Res.id.et_write_phone);
				etPhoneNum.setText("");
				etPhoneNum.addTextChangedListener(this);
				etPhoneNum.requestFocus();
				if (etPhoneNum.getText().length() > 0) {
					btnNext.setEnabled(true);

					ivClear = (ImageView) activity.findViewById(Res.id.iv_clear);
					ivClear.setVisibility(View.VISIBLE);
					resId = getBitmapRes(activity, "smssdk_btn_enable");
					if (resId > 0) {
						btnNext.setBackgroundResource(resId);
					}
				}

				ivClear = (ImageView) activity.findViewById(Res.id.iv_clear);

				llBack.setOnClickListener(this);
				btnNext.setOnClickListener(this);
				ivClear.setOnClickListener(this);
				viewCountry.setOnClickListener(this);

				handler = new EventHandler() {
					public void afterEvent(final int event, final int result,
							final Object data) {
						runOnUIThread(new Runnable() {
							public void run() {
								if (pd != null && pd.isShowing()) {
									pd.dismiss();
								}
								if (result == SMSSDK.RESULT_COMPLETE) {
									if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
										// ������֤�����ת����֤����дҳ��
										boolean smart = (Boolean)data;
										afterVerificationCodeRequested(smart);
									}
								} else {
									if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE
											&& data != null
											&& (data instanceof UserInterruptException)) {
										// ���ڴ˴��ǿ������Լ�����Ҫ�жϷ��͵ģ����ʲô��������
										return;
									}

									int status = 0;
									// ���ݷ��������ص�������󣬸�toast��ʾ
									try {
										((Throwable) data).printStackTrace();
										Throwable throwable = (Throwable) data;

										JSONObject object = new JSONObject(
												throwable.getMessage());
										String des = object.optString("detail");
										status = object.optInt("status");
										if (!TextUtils.isEmpty(des)) {
											Toast.makeText(activity, des, Toast.LENGTH_SHORT).show();
											return;
										}
									} catch (Exception e) {
										SMSLog.getInstance().w(e);
									}
									// ���ľ���ҵ���Դ��Ĭ����ʾ
									int resId = 0;
									if(status >= 400) {
										resId = getStringRes(activity,
												"smssdk_error_desc_"+status);
									} else {
										resId = getStringRes(activity,
												"smssdk_network_error");
									}

									if (resId > 0) {
										Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
									}
								}
							}
						});
					}
				};
			}

		}

		private String[] getCurrentCountry() {
			String mcc = getMCC();
			String[] country = null;
			if (!TextUtils.isEmpty(mcc)) {
				country = SMSSDK.getCountryByMCC(mcc);
			}

			if (country == null) {
				Log.w("SMSSDK", "no country found by MCC: " + mcc);
				country = SMSSDK.getCountry(DEFAULT_COUNTRY_ID);
			}
			return country;
		}

		private String getMCC() {
			TelephonyManager tm = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
			// ���ص�ǰ�ֻ�ע���������Ӫ�����ڹ��ҵ�MCC+MNC. ���ûע�ᵽ�����Ϊ��.
			String networkOperator = tm.getNetworkOperator();
			if (!TextUtils.isEmpty(networkOperator)) {
				return networkOperator;
			}

			// ����SIM����Ӫ�����ڹ��ҵ�MCC+MNC. 5λ��6λ. ���û��SIM�����ؿ�
			return tm.getSimOperator();
		}

		public void onResume() {
			SMSSDK.registerEventHandler(handler);
		}

		public void onPause() {
			SMSSDK.unregisterEventHandler(handler);
		}

		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {

		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() > 0) {
				btnNext.setEnabled(true);
				ivClear.setVisibility(View.VISIBLE);
				int resId = getBitmapRes(activity, "smssdk_btn_enable");
				if (resId > 0) {
					btnNext.setBackgroundResource(resId);
				}
			} else {
				btnNext.setEnabled(false);
				ivClear.setVisibility(View.GONE);
				int resId = getBitmapRes(activity, "smssdk_btn_disenable");
				if (resId > 0) {
					btnNext.setBackgroundResource(resId);
				}
			}
		}

		public void afterTextChanged(Editable s) {

		}

		public void onClick(View v) {
			int id = v.getId();
			int id_ll_back = Res.id.ll_back;
			int id_rl_country = Res.id.rl_country;
			int id_btn_next = Res.id.btn_next;
			int id_iv_clear = Res.id.iv_clear;

			if (id == id_ll_back) {
				finish();
			} else if (id == id_rl_country) {
				// �����б�
				CountryPage countryPage = new CountryPage();
				countryPage.setCountryId(currentId);
				countryPage.showForResult(activity, null, this);
			} else if (id == id_btn_next) {
				// �����Ͷ�����֤��
				String phone = etPhoneNum.getText().toString().trim().replaceAll("\\s*", "");
				String code = tvCountryNum.getText().toString().trim();
				showDialog(phone, code);
			} else if (id == id_iv_clear) {
				// ����绰���������
				etPhoneNum.getText().clear();
			}
		}

		@SuppressWarnings("unchecked")
		public void onResult(HashMap<String, Object> data) {
			if (data != null) {
				int page = (Integer) data.get("page");
				if (page == 1) {
					// �����б���
					currentId = (String) data.get("id");
					String[] country = SMSSDK.getCountry(currentId);
					if (country != null) {
						currentCode = country[1];
						tvCountryNum.setText("+" + currentCode);
						tvCountry.setText(country[0]);
					}
				} else if (page == 2) {
					// ��֤��У�鷵��
					Object res = data.get("res");
					//Object smart = data.get("smart");

					HashMap<String, Object> phoneMap = (HashMap<String, Object>) data.get("phone");
					if (res != null && phoneMap != null) {
						int resId = getStringRes(activity, "smssdk_your_ccount_is_verified");
						if (resId > 0) {
							Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
						}

						if (callback != null) {
							callback.afterEvent(
									SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE,
									SMSSDK.RESULT_COMPLETE, phoneMap);
						}
						/*���������дһ��������ת��activity,����TESTActivity
						 *Intent testIntent = new Intent(mContext, TestActivity.class);
					     *startActivity(testIntent);
						 *������ֻ����ʾһ��Toast
						 * */
						Toast.makeText(activity, "I don't return !", Toast.LENGTH_SHORT).show();
//						finish();
					}
				}
			}
		}

		/** �ָ�绰���� */
		private String splitPhoneNum(String phone) {
			StringBuilder builder = new StringBuilder(phone);
			builder.reverse();
			for (int i = 4, len = builder.length(); i < len; i += 5) {
				builder.insert(i, ' ');
			}
			builder.reverse();
			return builder.toString();
		}

		/** �Ƿ���������֤�룬�Ի��� */
		public void showDialog(final String phone, final String code) {
			int resId = getStyleRes(activity, "CommonDialog");
			if (resId > 0) {
				final String phoneNum = "+" + code + " " + splitPhoneNum(phone);
				final Dialog dialog = new Dialog(getContext(), resId);

				LinearLayout layout = SendMsgDialogLayout.create(getContext());

				if (layout != null) {
					dialog.setContentView(layout);

					((TextView) dialog.findViewById(Res.id.tv_phone)).setText(phoneNum);
					TextView tv = (TextView) dialog.findViewById(Res.id.tv_dialog_hint);
					resId = getStringRes(activity, "smssdk_make_sure_mobile_detail");
					if (resId > 0) {
						String text = getContext().getString(resId);

						tv.setText(Html.fromHtml(text));
					}

					((Button) dialog.findViewById(Res.id.btn_dialog_ok)).setOnClickListener(
							new OnClickListener() {
									public void onClick(View v) {
										// ��ת����֤��ҳ��
										dialog.dismiss();

										if (pd != null && pd.isShowing()) {
											pd.dismiss();
										}
										pd = CommonDialog.ProgressDialog(activity);
										if (pd != null) {
											pd.show();
										}
										Log.e("verification phone ==>>", phone);
										SMSSDK.getVerificationCode(code, phone.trim(), osmHandler);
									}
								});


						((Button) dialog.findViewById(Res.id.btn_dialog_cancel)).setOnClickListener(
								new OnClickListener() {
									public void onClick(View v) {
										dialog.dismiss();
									}
								});
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				}
			}
		}

		/** ������֤�����ת����֤����дҳ�� */
		private void afterVerificationCodeRequested(boolean smart) {
			String phone = etPhoneNum.getText().toString().trim().replaceAll("\\s*", "");
			String code = tvCountryNum.getText().toString().trim();
			if (code.startsWith("+")) {
				code = code.substring(1);
			}
			String formatedPhone = "+" + code + " " + splitPhoneNum(phone);

			// ��֤��ҳ��
			if(smart) {
				SmartVerifyPage smartPage = new SmartVerifyPage();
				smartPage.setPhone(phone, code, formatedPhone);
				smartPage.showForResult(activity, null, this);
			} else {
				IdentifyNumPage page = new IdentifyNumPage();
				page.setPhone(phone, code, formatedPhone);
				page.showForResult(activity, null, this);
			}
		}

}
