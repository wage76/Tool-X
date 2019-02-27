package com.asfoundation.wallet.ui.transact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.asf.wallet.R
import com.asfoundation.wallet.ui.BaseActivity

class TransactActivity : BaseActivity(), TransactActivityView, TransactNavigator {
  private lateinit var presenter: TransactActivityPresenter

  companion object {
    @JvmStatic
    fun newIntent(context: Context): Intent {
      return Intent(context, TransactActivity::class.java)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.transaction_activity_layout)
    presenter = TransactActivityPresenter(this)
    presenter.present(savedInstanceState == null)
    toolbar()
  }

  override fun showTransactFragment() {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, TransactFragment.newInstance()).commit()
  }

  override fun openAppcoinsCreditsSuccess() {
    supportFragmentManager.beginTransaction()
        .replace(R.id.fragment_container, AppcoinsCreditsTransactSuccessFragment.newInstance())
        .commit()
  }
}