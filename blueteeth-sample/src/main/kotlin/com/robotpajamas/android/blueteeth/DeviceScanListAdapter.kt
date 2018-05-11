package com.robotpajamas.android.blueteeth

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import com.robotpajamas.blueteeth.BlueteethDevice

import java.util.ArrayList

//import butterknife.BindView;
//import butterknife.ButterKnife;

class DeviceScanListAdapter(context: Context) : BaseAdapter() {

    private val mLayoutInflater: LayoutInflater
    private val mDevices: MutableList<BlueteethDevice>

    init {
        mLayoutInflater = LayoutInflater.from(context)
        mDevices = ArrayList()
    }

    override fun getCount(): Int {
        return mDevices.size
    }

    override fun getItem(position: Int): BlueteethDevice {
        return mDevices[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: DeviceHolder
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.listitem_device_scan, parent, false)
            //            holder = new DeviceHolder(convertView);
            //            convertView.setTag(holder);
        } else {
            holder = convertView.tag as DeviceHolder
        }

        val device = getItem(position)
        //        holder.deviceName.setText(device.getName());
        //        holder.deviceMac.setText(device.getMacAddress());

        return convertView!!
    }

    fun add(device: BlueteethDevice) {
        // Add only unique devices
        var isAlreadyInList = false
        for (d in mDevices) {
            if (device.macAddress == d.macAddress) {
                isAlreadyInList = true
                break
            }
        }

        if (!isAlreadyInList) {
            mDevices.add(device)
            notifyDataSetChanged()
        }
    }

    fun clear() {
        mDevices.clear()
        notifyDataSetChanged()
    }

    class DeviceHolder {

        //        @BindView(R.id.textview_device_name)
        internal var deviceName: TextView? = null

        //        @BindView(R.id.textview_device_mac)
        internal var deviceMac: TextView? = null

        //        public DeviceHolder(View view) {
        //            ButterKnife.bind(this, view);
        //        }
    }
}
