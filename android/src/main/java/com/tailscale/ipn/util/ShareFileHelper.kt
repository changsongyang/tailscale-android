// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import libtailscale.Libtailscale

/** Helper for working with shared files via the Storage Access Framework. */
object ShareFileHelper : libtailscale.ShareFileHelper {
  private var appContext: Context? = null
  private var savedUri: String? = null

  @JvmStatic
  fun init(context: Context, uri: String) {
    appContext = context.applicationContext
    savedUri = uri
    Libtailscale.setShareFileHelper(this)
  }

  /**
   * Opens a file for writing in the previously selected SAF directory.
   *
   * This creates a new file with the given [fileName] using the MIME type
   * "application/octet-stream" and returns the file descriptor for the newly created file.
   *
   * @return The file descriptor for the file, or -1 if the operation fails.
   */
  override fun openFileForWriting(fileName: String): Int {
    val context = appContext ?: return -1
    val directoryUriString = savedUri ?: return -1
    val uri = Uri.parse(directoryUriString)
    val pickedDir = DocumentFile.fromTreeUri(context, uri) ?: return -1
    val newFile = pickedDir.createFile("application/octet-stream", fileName) ?: return -1
    val pfd = context.contentResolver.openFileDescriptor(newFile.uri, "w")
    return pfd?.fd ?: -1
  }

  override fun renamePartialFile(
      partialUri: String, // URI of the partial file as a string
      targetDirUri: String, // The directory URI as a string
      targetName: String
  ): String {
    val context = appContext ?: return ""
    val partialUriObj = Uri.parse(partialUri)
    val targetDirUriObj = Uri.parse(targetDirUri)

    // Get a DocumentFile representing the target directory.
    val targetDir = DocumentFile.fromTreeUri(context, targetDirUriObj) ?: return ""
    var finalTargetName = targetName

    // Check if a file with the target name already exists and generate a new filename in case of
    // conflict.
    var destFile = targetDir.findFile(finalTargetName)
    if (destFile != null) {
      finalTargetName = generateNewFilename(finalTargetName)
    }

    destFile = targetDir.createFile("application/octet-stream", finalTargetName) ?: return ""

    // Copy contents from the partial file to the new file.
    context.contentResolver.openInputStream(partialUriObj)?.use { input ->
      context.contentResolver.openOutputStream(destFile.uri)?.use { output -> input.copyTo(output) }
          ?: return ""
    } ?: return ""

    // Delete the partial file.
    val partialFile = DocumentFile.fromSingleUri(context, partialUriObj)
    partialFile?.delete()

    return destFile.uri.toString()
  }

  fun generateNewFilename(filename: String): String {
    // Find the last dot, which separates the base name from the extension.
    val dotIndex = filename.lastIndexOf('.')
    val baseName: String
    val extension: String
    if (dotIndex != -1) {
      baseName = filename.substring(0, dotIndex)
      extension = filename.substring(dotIndex) // includes the dot
    } else {
      baseName = filename
      extension = ""
    }
    // Append a unique suffix using the current timestamp.
    return "$baseName-${System.currentTimeMillis()}$extension"
  }
}
