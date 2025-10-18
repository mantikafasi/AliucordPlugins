# FFmpeg Plugin

Automatically converts HEIC (High Efficiency Image Container) images to JPEG format before uploading to Discord.

## Features

- Automatic conversion of HEIC/HEIF images to JPEG
- High-quality conversion (quality level 2)
- Transparent operation - no user interaction required
- Uses FFmpeg for reliable conversion

## How it works

The plugin hooks into Discord's file upload process and intercepts HEIC files before they are uploaded. When a HEIC file is detected, it:

1. Converts the HEIC file to JPEG format using FFmpeg
2. Replaces the original file with the converted JPEG
3. Uploads the JPEG file instead

## Technical Details

- Uses the `mobile-ffmpeg-full` library for conversion
- Hooks into `kotlin.io.FilesKt.readBytes()` to intercept file uploads
- Maintains high image quality (FFmpeg quality setting: 2)
- Converted files are saved with `_converted.jpg` suffix

## Requirements

- Aliucord
- Android device with sufficient storage for temporary conversions

## Notes

- HEIC files are automatically detected by file extension (.heic or .heif)
- Original HEIC files are preserved; converted files are created separately
- The plugin logs conversion activities for debugging purposes
