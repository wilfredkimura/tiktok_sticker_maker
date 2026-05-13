import yt_dlp
import sys

def test_extract(url):
    ydl_opts = {
        'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        'quiet': True,
        'no_warnings': True,
        'simulate': True,
        'extract_flat': False,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        video_url = info.get("url")
        if not video_url:
            for f in info.get("formats", []):
                if f.get("ext") == "mp4" and f.get("url"):
                    video_url = f.get("url")
                    break
        print(f"Title: {info.get('title')}")
        print(f"Video URL: {video_url}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        test_extract(sys.argv[1])
    else:
        print("Please provide a URL")
