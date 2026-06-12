package com.example.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object FileHelperUtils {

    /**
     * Categorizes files into 'Documents', 'Media', and 'Archives'
     * utilizing basic file extension mapping to improve organization in the file list.
     */
    fun categorizeByExtension(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase().trim()
        
        return when (extension) {
            // Documents mapping
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "html", "htm" -> "Documents"
            
            // Media mapping
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "tiff", "svg",
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "3gp", "mpeg", "mpg",
            "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "mid" -> "Media"
            
            // Archives mapping
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "tgz" -> "Archives"
            
            // Fallback for everything else
            else -> "Other"
        }
    }

    /**
     * Shows a user-friendly Toast message on the Main UI thread for API and Service exceptions,
     * tailoring the message appropriately for Network or Authentication failures.
     */
    fun handleApiError(context: Context, error: Throwable, serviceName: String) {
        val message = when {
            // Check specifically for network issues
            error is UnknownHostException || error is SocketTimeoutException || error.cause is UnknownHostException -> {
                if (serviceName.equals("Gemini", ignoreCase = true)) {
                    "नारद AI: नेटवर्क समस्या! कृपया अपना इंटरनेट कनेक्शन जांचें।"
                } else {
                    "Google Drive: नेटवर्क कनेक्टिविटी त्रुटि! क्लाउड सर्वर से संपर्क नहीं हो सका।"
                }
            }
            
            error is IOException -> {
                if (serviceName.equals("Gemini", ignoreCase = true)) {
                    "नारद AI: डेटा ट्रांसफर विफलता। कनेक्टिविटी जांचें।"
                } else {
                    "Google Drive: फ़ाइल ट्रांसफर एरर! कृपया इंटरनेट की जांच करें।"
                }
            }
            
            // Check for HTTP errors (like Retrofit's HttpException)
            error.javaClass.simpleName.contains("HttpException") -> {
                val errorStr = error.toString()
                when {
                    errorStr.contains("401") || errorStr.contains("403") -> {
                        if (serviceName.equals("Gemini", ignoreCase = true)) {
                            "नारद AI: अनधिकृत अनुरोध। कृपया अपनी Gemini API Key जांचें!"
                        } else {
                            "Google Drive: प्रमाणीकरण त्रुटि! कृपया Google खाते से पुन: लॉगिन करें।"
                        }
                    }
                    errorStr.contains("429") -> {
                        "नारद AI: अनुरोध सीमा समाप्त (Rate Limit)! कृपया कुछ देर बाद प्रयास करें।"
                    }
                    else -> {
                        "$serviceName सर्वर एरर: (${errorStr.takeLast(10)})"
                    }
                }
            }
            
            // Generic fallback
            else -> {
                val msg = error.localizedMessage ?: error.message ?: ""
                when {
                    msg.contains("API key", ignoreCase = true) || msg.contains("API_KEY", ignoreCase = true) -> {
                        "नारद AI: अमान्य API कुंजी! कृपया Settings में सही API की प्रदान करें।"
                    }
                    msg.contains("Auth", ignoreCase = true) || msg.contains("sign", ignoreCase = true) -> {
                        "Google Drive: लॉगिन / प्रमाणीकरण की आवश्यकता है!"
                    }
                    else -> {
                        "$serviceName विफलता: ${msg.take(50)}"
                    }
                }
            }
        }

        // Post toast safely to main thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
