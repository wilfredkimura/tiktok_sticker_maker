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

import httpx

@app.post("/resolve", response_model=ResolveResponse)
async def resolve_video(request: ResolveRequest):
    logger.info(f"Resolving URL: {request.url}")
    
    # Strategy 1: Try TikWM API
    try:
        async with curl_requests.AsyncSession(impersonate="chrome120") as s:
            tikwm_url = f"https://www.tikwm.com/api/?url={request.url}"
            response = await s.get(tikwm_url, timeout=20)
            if response.status_code == 200:
                data = response.json()
                if data.get("code") == 0 and "data" in data:
                    video_url = data["data"].get("play") or data["data"].get("hdplay")
                    title = data["data"].get("title", "TikTok Video")
                    if video_url:
                        logger.info("Successfully resolved via TikWM")
                        encoded_url = base64.urlsafe_b64encode(video_url.encode()).decode()
                        return ResolveResponse(video_url=f"/proxy?url={encoded_url}", title=title)
    except Exception as e:
        logger.warning(f"TikWM resolution failed: {str(e)}")

    # Strategy 2: Fallback to yt-dlp
    ydl_opts = {
        'format': 'bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best',
        'quiet': True,
        'no_warnings': True,
        'simulate': True,
        'cookiefile': 'cookies.txt',
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
                raise HTTPException(status_code=400, detail="Could not extract video URL from yt-dlp")
                
            encoded_url = base64.urlsafe_b64encode(video_url.encode()).decode()
            return ResolveResponse(video_url=f"/proxy?url={encoded_url}", title=title)
            
    except Exception as e:
        logger.error(f"Resolution failed: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Resolution failed: {str(e)}")

@app.get("/proxy")
async def proxy_video(url: str):
    try:
        actual_url = base64.urlsafe_b64decode(url).decode()
        logger.info(f"Streaming proxy for: {actual_url[:60]}...")
        
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': 'https://www.tiktok.com/',
        }

        async def stream_video():
            async with httpx.AsyncClient(follow_redirects=True) as client:
                async with client.stream("GET", actual_url, headers=headers, timeout=60) as resp:
                    if resp.status_code != 200:
                        logger.error(f"Upstream returned {resp.status_code}")
                        return
                    async for chunk in resp.aiter_bytes(chunk_size=32768):
                        yield chunk

        return StreamingResponse(stream_video(), media_type="video/mp4")
        
    except Exception as e:
        logger.error(f"Proxy error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
def read_root():
    return {"status": "ok", "message": "TikTok Resolver API is running"}
