// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package libtailscale

import "fmt"

// AndroidFileOps implements taildrop.FileOps using a ShareFileHelper.
type AndroidFileOps struct {
	helper ShareFileHelper
}

func NewAndroidFileOps(helper ShareFileHelper) *AndroidFileOps {
	return &AndroidFileOps{helper: helper}
}

// GetSafFd calls the underlying ShareFileHelper's OpenFileForWriting.
func (ops *AndroidFileOps) GetSafFd(filename string) int32 {
	return ops.helper.OpenFileForWriting(filename)
}

// RenamePartialFile calls the helper's RenamePartialFile.
// It returns an error if the helper returns an empty string.
func (ops *AndroidFileOps) RenamePartialFile(partialUri, targetDirUri, targetName string) (string, error) {
	newURI := ops.helper.RenamePartialFile(partialUri, targetDirUri, targetName)
	if newURI == "" {
		return "", fmt.Errorf("failed to rename partial file via SAF")
	}
	return newURI, nil
}
