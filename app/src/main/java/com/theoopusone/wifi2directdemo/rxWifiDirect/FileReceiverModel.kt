package com.theoopusone.wifi2directdemo.rxWifiDirect

import android.content.Context
import android.util.Log
import com.theoopusone.wifi2directdemo.MainActivity
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.ServerSocket

class FileReceiverModel: FileCopyType {
    val receiveCompleteSubject = PublishSubject.create<Unit>()

    fun startFileReceiveServer(context: Context) {
        context.doAsync {
            GlobalScope.launch {
                Log.d(MainActivity.TAG, "start file server")

                try {
                    val serverSocket = ServerSocket(8988)
                    val client = serverSocket.accept()
                    val file = File(context.getExternalFilesDir("received"), "wifi2shared-${System.currentTimeMillis()}.png")
                    val dir = File(file.parent)
                    if (!dir.exists()) {
                        dir.mkdir()
                    }
                    file.createNewFile()

                    val inputStream = client.getInputStream()
                    copyFile(inputStream, FileOutputStream(file))
                    serverSocket.close()

                    receiveCompleteSubject.onNext(Unit)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}