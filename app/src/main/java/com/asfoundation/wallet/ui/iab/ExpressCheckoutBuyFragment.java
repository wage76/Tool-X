package com.asfoundation.wallet.ui.iab;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.asf.wallet.R;
import com.asfoundation.wallet.entity.FiatValueResponse;
import com.asfoundation.wallet.entity.TransactionBuilder;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxrelay2.PublishRelay;
import dagger.android.support.DaggerFragment;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Formatter;
import java.util.Locale;
import javax.inject.Inject;

/**
 * Created by franciscocalado on 20/07/2018.
 */

public class ExpressCheckoutBuyFragment extends DaggerFragment implements ExpressCheckoutBuyView {
  public static final String APP_PACKAGE = "app_package";
  public static final String PRODUCT_NAME = "product_name";
  @Inject InAppPurchaseInteractor inAppPurchaseInteractor;
  private Bundle extras;
  private String data;
  private PublishRelay<Snackbar> buyButtonClick;
  private IabView iabView;
  private ExpressCheckoutBuyPresenter presenter;
  private TextView appName;
  private TextView itemHeaderDescription;
  private TextView itemListDescription;
  private TextView itemPrice;
  private TextView itemFinalPrice;
  private ImageView appIcon;
  private Button buyButton;
  private Button cancelButton;

  public static ExpressCheckoutBuyFragment newInstance(Bundle extras, String uri) {
    ExpressCheckoutBuyFragment fragment = new ExpressCheckoutBuyFragment();
    Bundle bundle = new Bundle();
    bundle.putBundle("extras", extras);
    bundle.putString("data", uri);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    extras = getArguments().getBundle("extras");
    data = getArguments().getString("data");
  }

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_express_checkout_buy, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    appName = view.findViewById(R.id.app_name);
    itemHeaderDescription = view.findViewById(R.id.app_sku_description);
    itemListDescription = view.findViewById(R.id.sku_description);
    itemPrice = view.findViewById(R.id.sku_price);
    itemFinalPrice = view.findViewById(R.id.total_price);
    appIcon = view.findViewById(R.id.app_icon);
    buyButton = view.findViewById(R.id.buy_button);
    cancelButton = view.findViewById(R.id.cancel_button);
    presenter = new ExpressCheckoutBuyPresenter(this, inAppPurchaseInteractor,
        AndroidSchedulers.mainThread(), new CompositeDisposable());

    Single.defer(() -> Single.just(getAppPackage()))
        .observeOn(Schedulers.io())
        .map(packageName -> new Pair<>(getApplicationName(packageName),
            getContext().getPackageManager()
                .getApplicationIcon(packageName)))
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(pair -> {
          appName.setText(pair.first);
          appIcon.setImageDrawable(pair.second);
        }, throwable -> {
          throwable.printStackTrace();
          showError();
        });
    buyButton.setOnClickListener(
        v -> Snackbar.make(this.getView(), "Buy triggered", BaseTransientBottomBar.LENGTH_LONG)
            .show());
  }

  @Override public void onStart() {
    super.onStart();
    presenter.present(data, getAppPackage(), extras.getString(PRODUCT_NAME));
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  @Override public void onDestroy() {
    super.onDestroy();
  }

  @Override public void onDetach() {
    super.onDetach();
    iabView = null;
  }

  @Override public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof IabView)) {
      throw new IllegalStateException(
          "Express checkout buy fragment must be attached to IAB activity");
    }
    iabView = ((IabView) context);
  }

  @Override public void setup(TransactionBuilder transactionBuilder, FiatValueResponse response) {
    Formatter formatter = new Formatter();
    String valueText = formatter.format(Locale.getDefault(), "%(,.2f", transactionBuilder.amount()
        .doubleValue())
        .toString() + " APPC";
    String valueTextCompose = valueText + " = ";
    String currency = getCurrency(response.getCurrency());
    String priceText = currency + Double.toString(response.getAmount());
    String finalString = valueTextCompose + priceText;
    Spannable spannable = new SpannableString(finalString);
    spannable.setSpan(new AbsoluteSizeSpan(12, true), finalString.indexOf(valueTextCompose),
        finalString.indexOf(valueTextCompose) + valueTextCompose.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    spannable.setSpan(
        new ForegroundColorSpan(getResources().getColor(R.color.dialog_buy_total_value)),
        finalString.indexOf(priceText), finalString.indexOf(priceText) + priceText.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    itemPrice.setText(valueText);
    itemFinalPrice.setText(spannable, TextView.BufferType.SPANNABLE);

    if (extras.containsKey(PRODUCT_NAME)) {
      itemHeaderDescription.setText(extras.getString(PRODUCT_NAME));
      itemListDescription.setText(extras.getString(PRODUCT_NAME));
    }
  }

  @Override public void showError() {
  }

  @Override public Observable<Object> getCancelClick() {
    return RxView.clicks(cancelButton);
  }

  @Override public void close() {
    iabView.close();
  }

  private CharSequence getApplicationName(String appPackage)
      throws PackageManager.NameNotFoundException {
    PackageManager packageManager = getContext().getPackageManager();
    ApplicationInfo packageInfo = packageManager.getApplicationInfo(appPackage, 0);
    return packageManager.getApplicationLabel(packageInfo);
  }

  public String getAppPackage() {
    if (extras.containsKey(APP_PACKAGE)) {
      return extras.getString(APP_PACKAGE);
    }
    throw new IllegalArgumentException("previous app package name not found");
  }

  public String getCurrency(String currency) {
    switch (currency) {
      case "EUR":
        return "€";
      default:
        return "$";
    }
  }
}
