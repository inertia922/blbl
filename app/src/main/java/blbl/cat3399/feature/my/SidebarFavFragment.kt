package blbl.cat3399.feature.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import blbl.cat3399.R
import blbl.cat3399.core.net.BiliClient
import blbl.cat3399.databinding.FragmentMyContainerBinding
import blbl.cat3399.ui.BackPressHandler
import blbl.cat3399.ui.RefreshKeyHandler

class SidebarFavFragment : Fragment(), MyNavigator, BackPressHandler, RefreshKeyHandler {
    private var _binding: FragmentMyContainerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            showContentForLoginState()
        }
    }

    override fun onResume() {
        super.onResume()
        if (childFragmentManager.backStackEntryCount == 0) {
            showContentForLoginState()
        }
    }

    override fun handleRefreshKey(): Boolean {
        val current = childFragmentManager.findFragmentById(R.id.my_container)
        return (current as? RefreshKeyHandler)?.handleRefreshKey() == true
    }

    private fun showContentForLoginState() {
        if (_binding == null || childFragmentManager.isStateSaved) return
        val wantLogin = !BiliClient.cookies.hasSessData()
        val current = childFragmentManager.findFragmentById(R.id.my_container)
        if (wantLogin && current is MyLoginFragment) return
        if (!wantLogin && current is MyFavFoldersFragment) return

        val fragment = if (wantLogin) MyLoginFragment() else MyFavFoldersFragment()
        childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.my_container, fragment)
            .commit()
    }

    override fun openFavFolder(mediaId: Long, title: String) {
        if (_binding == null || childFragmentManager.isStateSaved) return
        childFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.my_container, MyFavFolderDetailFragment.newInstance(mediaId = mediaId, title = title))
            .addToBackStack(null)
            .commit()
    }

    override fun openBangumiDetail(
        seasonId: Long,
        isDrama: Boolean,
        continueEpId: Long?,
        continueEpIndex: Int?,
    ) {
    }

    override fun handleBackPressed(): Boolean {
        if (childFragmentManager.popBackStackImmediate()) return true
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
