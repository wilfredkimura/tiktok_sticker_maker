from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import yt_dlp
import traceback
import logging

# Configure basic logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="TikTok Video Resolver API")

class ResolveRequest(BaseModel):
    url: str

class ResolveResponse(BaseModel):
    video_url: str
    title: str

@app.post("/resolve", response_model=ResolveResponse)
async def resolve_video(request: ResolveRequest):
    # yt-dlp options to extract the best MP4 format without downloading
    ydl_opts = {
        'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        'quiet': True,
        'no_warnings': True,
        'simulate': True, # Do not download the video
        'extract_flat': False, # We need the actual media URLs
        'extractor_args': {'tiktok': ['api_hostname=api16-normal-c-useast1a.tiktokv.com']},
        'cookiefile': 'cookies.txt', # yt-dlp will look for this file
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
            'Accept-Language': 'en-US,en;q=0.9',
            'Sec-Fetch-Mode': 'navigate',
        }
    }

    try:
        logger.info(f"Resolving URL: {request.url}")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Extract info dictionary
            info = ydl.extract_info(request.url, download=False)
            
            # The 'url' field typically contains the direct download link
            video_url = info.get("url")
            title = info.get("title", "TikTok Video")
            
            # Fallback to search through formats if 'url' is not at the top level
            if not video_url:
                formats = info.get("formats", [])
                for f in formats:
                    if f.get("ext") == "mp4" and f.get("url"):
                        video_url = f.get("url")
                        break
                        
            if not video_url:
                raise HTTPException(status_code=400, detail="Could not extract direct video URL")
                
            return ResolveResponse(video_url=video_url, title=title)
            
    except Exception as e:
        logger.error(f"Error resolving {request.url}: {str(e)}")
        logger.error(traceback.format_exc())
        raise HTTPException(status_code=400, detail=f"Failed to process URL: {str(e)}")

@app.get("/")
def read_root():
    return {"status": "ok", "message": "TikTok Resolver API is running"}
