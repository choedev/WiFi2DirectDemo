package com.theoopusone.wifi2directdemo

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.android.synthetic.main.peer_list_item.view.*

class PeerListAdapter(private val itemClickSubject: PublishSubject<WifiP2pDevice>) : RecyclerView.Adapter<PeerListAdapter.PeerViewHolder>() {
    private var items: List<WifiP2pDevice> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        return PeerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.peer_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        items[position].let { device ->
            with(holder) {
                deviceName.text = device.deviceName
                deviceAddress.text = device.deviceAddress
                connect.setOnClickListener {
                    itemClickSubject.onNext(items[position])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateItems(itemList: List<WifiP2pDevice>) {
        items = itemList
        notifyDataSetChanged()
    }

    inner class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName = itemView.deviceName
        val deviceAddress = itemView.deviceAddress
        val connect = itemView.connect
    }
}