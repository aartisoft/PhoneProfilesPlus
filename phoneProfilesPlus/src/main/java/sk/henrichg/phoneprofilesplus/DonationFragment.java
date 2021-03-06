package sk.henrichg.phoneprofilesplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

import sk.henrichg.phoneprofilesplus.billing.BillingProvider;

//import me.drakeet.support.toast.ToastCompat;

public class DonationFragment extends Fragment {

    private List<SkuDetails> SKU_DETAILS = null;

    private ProgressBar mLoadingView;
    private TextView mErrorTextView;
    private GridView mGoogleGridView;
    //private AppCompatSpinner mGoogleSpinner;
    //private Button btGoogle;

    private BillingProvider mBillingProvider;

    // Debug tag, for logging
    //private static final String TAG = "DonationFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this is really important in order to save the state across screen
        // configuration changes for example
        setRetainInstance(true);
    }

    /*
    public static DonationFragment newInstance() {
        return new DonationFragment();
    }
    */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //PPApplication.logE(TAG, "onCreateView");
        View root = inflater.inflate(R.layout.donation_fragment, container, false);

        mLoadingView = root.findViewById(R.id.donation_google_android_market_loading);

        /*
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) && (getActivity() != null)) {
            TypedValue typedValue = new TypedValue();
            TypedArray a = getActivity().obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorAccent});
            int accentColor = a.getColor(0, 0);
            a.recycle();
            mLoadingView.getIndeterminateDrawable().setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        }
        */

        mErrorTextView = root.findViewById(R.id.donation_google_android_market_error_textview);

        // choose donation amount
        mGoogleGridView = root.findViewById(R.id.donation_google_android_market_grid);


//        mGoogleSpinner = root.findViewById(R.id.donation_google_android_market_spinner);
//        mGoogleSpinner.setPopupBackgroundResource(R.drawable.popupmenu_background);
/*        switch (ApplicationPreferences.applicationTheme(getActivity(), true)) {
            case "dark":
                mGoogleSpinner.setPopupBackgroundResource(R.drawable.popupmenu_background_dark);
                break;
            case "white":
                mGoogleSpinner.setPopupBackgroundResource(R.drawable.popupmenu_background_white);
                break;
//            case "dlight":
//                mGoogleSpinner.setPopupBackgroundResource(R.drawable.popupmenu_background_dlight);
//                break;
            default:
                mGoogleSpinner.setPopupBackgroundResource(R.drawable.popupmenu_background_white);
                break;
        }*/

//        btGoogle = root.findViewById(
//                R.id.donation_google_android_market_donate_button);
//        btGoogle.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                donateGoogleOnClick(-1);
//            }
//        });

        mBillingProvider = (BillingProvider) getActivity();

        /*
        Button paypalButton = root.findViewById(R.id.donation_paypal_donate_button);
        paypalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mErrorTextView.setVisibility(View.GONE);
                String url = "https://www.paypal.me/HenrichGron";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.web_browser_chooser)));
                } catch (Exception ignored) {
                }
            }
        });
        */

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button closeButton = view.findViewById(R.id.donation_activity_close);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null)
                    getActivity().finish();
            }
        });
    }

    /**
     * Enables or disables "please wait" screen.
     */
    public void setWaitScreen(boolean set) {
        if (mLoadingView != null)
            mLoadingView.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    /**
     * Donate button executes donations based on selection in spinner
     */
    private void donateGoogleOnClick(int position) {
        //final int index = mGoogleSpinner.getSelectedItemPosition();

        mBillingProvider.getBillingManager().startPurchaseFlow(SKU_DETAILS.get(position));
        //mBillingProvider.getBillingManager().startPurchaseFlow(SKU_DETAILS.get(index).getSku(), BillingClient.SkuType.INAPP);
    }

    public void updateGUIAfterBillingConnected() {
        // Start querying for SKUs
        //PPApplication.logE(TAG, "handleManagerAndUiReady");
        final List<String> inAppSkus = mBillingProvider.getBillingManager()
                .getSkus(/*!mDebug, */BillingClient.SkuType.INAPP);
        mBillingProvider.getBillingManager().querySkuDetailsAsync(BillingClient.SkuType.INAPP,
                inAppSkus,
                new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        int responseCode = billingResult.getResponseCode();
                        //PPApplication.logE(TAG, "onSkuDetailsResponse responseCode="+responseCode);

                        String[] prices = new String[]{"1 €", "2 €", "3 €", "5 €", "8 €", "13 €", "20 €"};

                        //if (skuDetailsList != null)
                        //    PPApplication.logE(TAG, "onSkuDetailsResponse skuDetailsList="+skuDetailsList.size());
                        if (responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                            if (skuDetailsList.size() > 0) {
                                SKU_DETAILS = new ArrayList<>();
                                for (int i = 0; i < inAppSkus.size(); i++) {
                                    for (int j = 0; j < skuDetailsList.size(); j++) {
                                        if (skuDetailsList.get(j).getSku().equals(inAppSkus.get(i))) {
                                            //PPApplication.logE(TAG, "Found sku: " + skuDetailsList.get(j));
                                            SKU_DETAILS.add(skuDetailsList.get(j));
                                            prices[i] = skuDetailsList.get(j).getPrice();
                                            break;
                                        }
                                    }
                                }

                                // update the UI
                                displayAnErrorIfNeeded(BillingClient.BillingResponseCode.OK);

                                if (getActivity() != null) {
                                    mGoogleGridView.setAdapter(new DonationGooglePlayAdapter(DonationFragment.this, prices));
                                    mGoogleGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                            donateGoogleOnClick(position);
                                        }


                                    });
                                    mGoogleGridView.setEnabled(true);

//                                    GlobalGUIRoutines.HighlightedSpinnerAdapter spinnerAdapter = new GlobalGUIRoutines.HighlightedSpinnerAdapter(
//                                            getActivity(),
//                                            R.layout.highlighted_spinner,
//                                            prices);
//                                    mGoogleSpinner.setSupportBackgroundTintList(ContextCompat.getColorStateList(getActivity().getBaseContext(), R.color.accent));
//                                    spinnerAdapter.setDropDownViewResource(R.layout.highlighted_spinner_dropdown);
//                                    mGoogleSpinner.setAdapter(spinnerAdapter);
//
//                                    mGoogleSpinner.setSelection(0);
//                                    mGoogleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//
//                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                                            ((GlobalGUIRoutines.HighlightedSpinnerAdapter)mGoogleSpinner.getAdapter()).setSelection(position);
//                                        }
//
//                                        public void onNothingSelected(AdapterView<?> parent) {
//                                        }
//                                    });
//
//                                    btGoogle.setEnabled(true);
                                }
                            }
                            else {
                                displayAnErrorIfNeeded(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED);
                            }
                        }
                    }
                });
    }

    public void purchaseSuccessful(@SuppressWarnings("unused") List<Purchase> purchases) {
/*        if (purchases != null) {
            for (Purchase purchase : purchases) {
                String sku = purchase.getSku();
                for (SkuDetails skuDetail : SKU_DETAILS) {
                    if (skuDetail.getSku().equals(sku)) {
                        if (PPApplication.logEnabled()) {
                            PPApplication.logE(TAG, "purchaseSuccessful - sku=" + sku);
                            PPApplication.logE(TAG, "purchaseSuccessful - currency=" + skuDetail.getPriceCurrencyCode());
                            PPApplication.logE(TAG, "purchaseSuccessful - priceS=" + skuDetail.getPrice());
                            PPApplication.logE(TAG, "purchaseSuccessful - priceMicros=" + skuDetail.getPriceAmountMicros());
                            PPApplication.logE(TAG, "purchaseSuccessful - price=" + skuDetail.getPriceAmountMicros() / 1000000.0);
                        }
//                        Answers.getInstance().logPurchase(new PurchaseEvent()
//                                .putItemPrice(BigDecimal.valueOf(skuDetail.getPriceAmountMicros() / 1000000.0))
//                                .putCurrency(Currency.getInstance(skuDetail.getPriceCurrencyCode()))
//                                .putItemName("Donation")
//                                //.putItemType("Apparel")
//                                .putItemId(sku)
//                                .putSuccess(true));
                    }
                }
            }
        }
*/
        if (getActivity() != null) {
            PPApplication.setDonationDonated(getActivity().getApplicationContext());
            PPApplication.showToast(getActivity().getApplicationContext(), getString(R.string.donation_thanks_dialog), Toast.LENGTH_LONG);
        }
    }

    @SuppressWarnings("EmptyMethod")
    public void purchaseUnsuccessful(@SuppressWarnings("unused") List<Purchase> purchases) {
/*        if (purchases != null) {
            for (Purchase purchase : purchases) {
                String sku = purchase.getSku();
                for (SkuDetails skuDetail : SKU_DETAILS) {
                    if (skuDetail.getSku().equals(sku)) {
                        if (PPApplication.logEnabled()) {
                            PPApplication.logE(TAG, "purchaseUnsuccessful - sku=" + sku);
                            PPApplication.logE(TAG, "purchaseUnsuccessful - currency=" + skuDetail.getPriceCurrencyCode());
                            PPApplication.logE(TAG, "purchaseUnsuccessful - priceS=" + skuDetail.getPrice());
                            PPApplication.logE(TAG, "purchaseUnsuccessful - priceMicros=" + skuDetail.getPriceAmountMicros());
                            PPApplication.logE(TAG, "purchaseUnsuccessful - price=" + skuDetail.getPriceAmountMicros() / 1000000.0);
                        }
//                        Answers.getInstance().logPurchase(new PurchaseEvent()
//                                .putItemPrice(BigDecimal.valueOf(skuDetail.getPriceAmountMicros() / 1000000.0))
//                                .putCurrency(Currency.getInstance(skuDetail.getPriceCurrencyCode()))
//                                .putItemName("Donation")
//                                //.putItemType("Apparel")
//                                .putItemId(sku)
//                                .putSuccess(false));
                    }
                }
            }
        }*/
    }

    public void displayAnErrorIfNeeded(int response) {
        if (getActivity() == null || getActivity().isFinishing()) {
            //PPApplication.logE(TAG, "No need to show an error - activity is finishing already");
            return;
        }

        setWaitScreen(false);
        if (mErrorTextView != null) {
            if (response != BillingClient.BillingResponseCode.OK) {
                mErrorTextView.setVisibility(View.VISIBLE);
                switch (response) {
                    case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                        mErrorTextView.setText(R.string.donation_google_android_market_not_supported);
                        break;
                    case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                    case BillingClient.BillingResponseCode.ERROR:
                    case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                    case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                    case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                    case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                    case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                        mErrorTextView.setText(R.string.donation_google_error);
                        break;
                    case BillingClient.BillingResponseCode.USER_CANCELED:
                        mErrorTextView.setVisibility(View.GONE);
                        break;
                }
            } else
                mErrorTextView.setVisibility(View.GONE);
        }
    }

}
