package com.byagowi.persiancalendar.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannedString
import android.text.util.Linkify
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.byagowi.persiancalendar.*
import com.byagowi.persiancalendar.databinding.DialogEmailBinding
import com.byagowi.persiancalendar.databinding.FragmentAboutBinding
import com.byagowi.persiancalendar.ui.DrawerHost
import com.byagowi.persiancalendar.utils.*
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar

class AboutFragment : Fragment() {

    private val appVersionList
        get() = BuildConfig.VERSION_NAME.split("-")
            .mapIndexed { i, x -> if (i == 0) formatNumber(x) else x }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAboutBinding.inflate(inflater, container, false)

        binding.appBar.toolbar.let { toolbar ->
            toolbar.setTitle(R.string.about)
            (activity as? DrawerHost)?.setupToolbarWithDrawer(viewLifecycleOwner, toolbar)
            toolbar.menu.add(R.string.share).also {
                it.icon = toolbar.context.getCompatDrawable(R.drawable.ic_baseline_share)
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                it.onClick { shareApplication() }
            }
            toolbar.menu.add(R.string.device_info).also {
                it.icon = toolbar.context.getCompatDrawable(R.drawable.ic_device_information)
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                it.onClick {
                    findNavController().navigateSafe(AboutFragmentDirections.actionAboutToDeviceInformation())
                }
            }
        }
        binding.appBar.appbarLayout.hideToolbarBottomShadow()

        val isUserAbleToReadPersian = when (language) {
            LANG_FA, LANG_GLK, LANG_AZB, LANG_FA_AF, LANG_EN_IR -> true
            else -> false
        }

        // app
        val version: SpannedString = buildSpannedString {
            scale(1f) {
                bold {
                    append(getString(R.string.app_name))
                }
            }
            append("\n")
            scale(.8f) {
                append(getString(R.string.version).format(appVersionList.joinToString("-")))
            }
            if (isUserAbleToReadPersian) {
                append("\n")
                scale(.8f) {
                    append(
                        getString(R.string.about_help_subtitle).format(
                            formatNumber(supportedYearOfIranCalendar - 1),
                            formatNumber(supportedYearOfIranCalendar)
                        )
                    )
                }
            }
        }
        binding.aboutHeader.also {
            it.text = version
            it.fadeIn(2500)
            it.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.ic_launcher, 0, 0)
        }

        fun TextView.putLineStartIcon(@DrawableRes icon: Int) {
            if (isResourcesRTL(context)) setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
            else setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        }

        // licenses
        binding.licenses.setOnClickListener {
            findNavController().navigateSafe(AboutFragmentDirections.actionAboutToLicenses())
        }
        binding.licensesTitle.putLineStartIcon(R.drawable.ic_licences)

        // help
        binding.helpCard.isVisible = isUserAbleToReadPersian
        binding.helpTitle.putLineStartIcon(R.drawable.ic_help)
        binding.helpSectionsRecyclerView.apply {
            val sections = getString(R.string.help_sections)
                .split(Regex("^={4}$", RegexOption.MULTILINE))
                .map { it.trim().lines() }
                .map { lines ->
                    val content = SpannableString(lines.drop(1).joinToString("\n").trim())
                    Linkify.addLinks(content, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
                    ExpandableItemsAdapter.Item(lines.first(), null, content)
                }
            adapter = ExpandableItemsAdapter(sections, isRTL = true)
            layoutManager = LinearLayoutManager(context)
        }

        // report bug
        binding.reportBug.setOnClickListener { launchReportIntent() }
        binding.reportBugTitle.putLineStartIcon(R.drawable.ic_bug)

        binding.email.setOnClickListener { showEmailDialog() }
        binding.emailTitle.putLineStartIcon(R.drawable.ic_email)

        setupContributorsList(binding)

        return binding.root
    }

    private fun View.fadeIn(durationMillis: Long = 250) {
        this.startAnimation(AlphaAnimation(0F, 1F).apply {
            duration = durationMillis
            fillAfter = true
        })
    }

    private fun setupContributorsList(binding: FragmentAboutBinding) {
        val context = binding.root.context

        val chipsIconTintId = TypedValue().apply {
            context.theme.resolveAttribute(R.attr.colorDrawerIcon, this, true)
        }.resourceId

        val chipClick = View.OnClickListener {
            (it.tag as? String)?.also { user ->
                if (user == "ImanSoltanian") return@also // The only person without GitHub account
                runCatching {
                    val uri = "https://github.com/$user".toUri()
                    CustomTabsIntent.Builder().build().launchUrl(context, uri)
                }.onFailure(logException)
            }
        }

        listOf(
            R.string.about_developers_list to R.drawable.ic_developer,
            R.string.about_designers_list to R.drawable.ic_designer,
            R.string.about_translators_list to R.drawable.ic_translator,
            R.string.about_contributors_list to R.drawable.ic_developer
        ).flatMap { (listId: Int, iconId: Int) ->
            val icon = context.getCompatDrawable(iconId)
            getString(listId).trim().split("\n").map {
                Chip(context).also { chip ->
                    chip.setOnClickListener(chipClick)
                    val parts = it.split(": ")
                    chip.tag = parts[0]
                    chip.text = parts[1]
                    chip.chipIcon = icon
                    chip.setChipIconTintResource(chipsIconTintId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        chip.elevation = resources.getDimension(R.dimen.chip_elevation)
                    }
                }
            }
        }.shuffled().forEach(binding.developers::addView)
    }

    private fun launchReportIntent() {
        runCatching {
            val uri = "https://github.com/persian-calendar/DroidPersianCalendar/issues/new".toUri()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure(logException)
    }

    private fun showEmailDialog() {
        val emailBinding = DialogEmailBinding.inflate(layoutInflater)
        AlertDialog.Builder(layoutInflater.context)
            .setView(emailBinding.root)
            .setTitle(R.string.about_email_sum)
            .setPositiveButton(R.string.continue_button) { _, _ ->
                launchEmailIntent(emailBinding.inputText.text?.toString())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchEmailIntent(defaultMessage: String? = null) {
        val email = "persian-calendar-admin@googlegroups.com"
        val subject = getString(R.string.app_name)
        val body = """$defaultMessage




===Device Information===
Manufacturer: ${Build.MANUFACTURER}
Model: ${Build.MODEL}
Android Version: ${Build.VERSION.RELEASE}
App Version Code: ${appVersionList[0]}"""

        // https://stackoverflow.com/a/62597382
        val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$email?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}".toUri()
        }
        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            selector = selectorIntent
        }
        runCatching {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.about_sendMail)))
        }.onFailure(logException).onFailure {
            Snackbar.make(view ?: return, R.string.about_noClient, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun shareApplication() {
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                val textToShare = """${getString(R.string.app_name)}
https://github.com/persian-calendar/DroidPersianCalendar"""
                putExtra(Intent.EXTRA_TEXT, textToShare)
            }, getString(R.string.share)))
        }.onFailure(logException).onFailure { bringMarketPage(activity ?: return) }
    }
}
