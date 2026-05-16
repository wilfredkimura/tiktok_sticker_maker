from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from fastapi.responses import StreamingResponse
from curl_cffi import requests as curl_requests
import yt_dlp
import traceback
import logging
import os
import base64

# Configure basic logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Handle cookies.txt from environment variable (for Render deployment)
cookies_b64 = os.environ.get("TIKTOK_COOKIES_BASE64")
if cookies_b64:
    try:
        cookies_data = base64.b64decode(cookies_b64)
        with open("cookies.txt", "wb") as f:
            f.write(cookies_data)
        logger.info("Successfully decoded and created cookies.txt from environment variable")
    except Exception as e:
        logger.error(f"Failed to decode TIKTOK_COOKIES_BASE64: {str(e)}")

app = FastAPI(title="TikTok Video Resolver API")

class ResolveRequest(BaseModel):
    url: str

class ResolveResponse(BaseModel):
    video_url: str
    title: str

@app.post("/resolve", response_model=ResolveResponse)
async def resolve_video(request: ResolveRequest):
    logger.info(f"Resolving URL: {request.url}")
    
    # Strategy 1: Try TikWM API (Very reliable, bypasses most 429s)
    try:
        async with curl_requests.AsyncSession(impersonate="chrome120") as s:
            tikwm_url = f"https://www.tikwm.com/api/?url={request.url}"
            response = await s.get(tikwm_url, timeout=30)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 0:
                    video_url = data["data"].get("play") # Video without watermark
                    title = data["data"].get("title", "TikTok Video")
                    if video_url:
                        logger.info("Successfully resolved via TikWM")
                        encoded_url = base64.urlsafe_b64encode(video_url.encode()).decode()
                        return ResolveResponse(video_url=f"/proxy?url={encoded_url}", title=title)
    except Exception as e:
        logger.warning(f"TikWM resolution failed: {str(e)}")

    # Strategy 2: Fallback to yt-dlp (Original method)
    ydl_opts = {
        'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        'quiet': True,
        'no_warnings': True,
        'simulate': True,
        'extract_flat': False,
        'cookiefile': 'cookies.txt',
        'retries': 3,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36',
        }
    }

    try:
        logger.info("Attempting fallback resolution via yt-dlp")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(request.url, download=False)
            video_url = info.get("url")
            title = info.get("title", "TikTok Video")
            
            if not video_url:
                formats = info.get("formats", [])
                for f in formats:
                    if f.get("ext") == "mp4" and f.get("url"):
                        video_url = f.get("url")
                        break
                        
            if not video_url:
                raise HTTPException(status_code=400, detail="Could not extract direct video URL")
                
            encoded_url = base64.urlsafe_b64encode(video_url.encode()).decode()
            return ResolveResponse(video_url=f"/proxy?url={encoded_url}", title=title)
            
    except Exception as e:
        logger.error(f"All resolution strategies failed for {request.url}: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Failed to process URL: {str(e)}")

@app.get("/proxy")
async def proxy_video(url: str):
    try:
        # Decode the URL using URL-safe base64
        actual_url = base64.urlsafe_b64decode(url).decode()
        logger.info(f"Proxying request for: {actual_url[:50]}...")
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': 'https://www.tiktok.com/',
            'Accept': 'video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5',
            'Accept-Language': 'en-US,en;q=0.9',
        }

        # Use curl-cffi to impersonate a real browser TLS fingerprint
        async def stream_video():
            async with curl_requests.AsyncSession(impersonate="chrome120") as s:
                try:
                    # curl-cffi handle streaming differently. We fetch the content.
                    # TikTok videos are small (usually < 20MB), so we can fetch and yield.
                    # Impersonation happens automatically with the session.
                    response = await s.get(actual_url, headers=headers, timeout=60, allow_redirects=True)
                    
                    if response.status_code != 200:
                        logger.error(f"TikTok CDN returned {response.status_code} via curl-cffi")
                        return

                    # Yield the entire content as a single chunk for reliability
                    yield response.content
                except Exception as e:
                    logger.error(f"Proxy streaming error: {str(e)}")

        return StreamingResponse(stream_video(), media_type="video/mp4")
        
    except Exception as e:
        logger.error(f"Proxy setup error: {str(e)}")
        logger.error(traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
def read_root():
    return {"status": "ok", "message": "TikTok Resolver API is running"}
