package com.pdam.report.ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.LatLng
import com.pdam.report.R
import com.pdam.report.data.PresenceData
import com.pdam.report.databinding.ActivityDetailPresenceBinding
import com.pdam.report.utils.FullScreenImageDialogFragment
import com.pdam.report.utils.GeocoderHelper
import com.pdam.report.utils.milisToDateTime
import com.pdam.report.utils.navigatePage
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class DetailPresenceActivity : AppCompatActivity(), MapListener {

    private val binding: ActivityDetailPresenceBinding by lazy {
        ActivityDetailPresenceBinding.inflate(layoutInflater)
    }
    private lateinit var mMap: MapView
    private lateinit var mController: MapController

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigatePage(this@DetailPresenceActivity, AdminPresenceActivity::class.java)
            finish()
        }
    }


    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        supportActionBar?.apply {
            title = getString(R.string.detail_presensi)
            setDisplayHomeAsUpEnabled(true)
            setBackgroundDrawable(resources.getDrawable(R.color.tropical_blue))
        }

        // Mengambil data tambahan yang dikirim melalui Intent
        val presence = intent.getParcelableExtra<PresenceData>(EXTRA_DATA) as PresenceData

        initializeMap(presence.lat, presence.lng)

        binding.apply {
            tvName.text = presence.username
            tvTimestampe.text = "Diambil pada ${milisToDateTime(presence.currentDate)}"
            Glide.with(this@DetailPresenceActivity)
                .load(presence.photoUrl)
                .into(imgPhoto)
            imgPhoto.setOnClickListener {
                supportFragmentManager.beginTransaction()
                    .add(
                        FullScreenImageDialogFragment(presence.photoUrl),
                        "FullScreenImageDialogFragment"
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun initializeMap(lat: Double, lng: Double) {
        // Konfigurasi MapView
        mMap = binding.osmmap
        mController = mMap.controller as MapController
        mMap.setTileSource(TileSourceFactory.MAPNIK)
        mMap.setMultiTouchControls(true)
        mMap.setBuiltInZoomControls(true)
        mMap.minZoomLevel = 2.0
        mMap.maxZoomLevel = 20.0

        // Kontroler peta
        mController.setZoom(15.0)
        mController.setCenter(GeoPoint(lat, lng))

        // Tambahkan marker
        val startPoint = GeoPoint(lat, lng)
        val startMarker = Marker(mMap)
        startMarker.position = startPoint
        startMarker.title = GeocoderHelper(this).getAddressFromLatLng(LatLng(lat, lng))
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mMap.overlays.add(startMarker)
    }


    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        return false
    }

    companion object {
        // Konstanta yang digunakan untuk mengirim data tambahan melalui Intent
        const val EXTRA_DATA = "extra_data"
    }

}