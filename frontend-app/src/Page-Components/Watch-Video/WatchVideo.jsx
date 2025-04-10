import React, { useEffect, useRef, useState } from "react";
import './WatchVideo.css';
import { useSearchParams } from "react-router-dom";
import { StreamingService } from '../../Service/StreamingService';
import like from '../../../public/Images/like.png';
import dislike from '../../../public/Images/dislike.png';
import share from '../../../public/Images/share.svg';
import profile from '../../../public/Images/profile.svg';
import ReactPlayer from 'react-player';

function WatchVideo() {
    const [searchParams] = useSearchParams();

    const guid = searchParams.get("v");
    const playback = searchParams.get("playback") || 0;

    const playerURL = "http://localhost:8092/streaming/video_file/"+guid;
    const [videoBlobUrl, setVideoBlobUrl] = useState(null);

    const streamingService = new StreamingService();

    const [isLiked, setIsLiked] = useState(0);
    const [isDisliked, setIsDisliked] = useState(0);
    const [is_processed, set_is_processed] = useState(true);
    const [channel_name, set_channel_name] = useState("");
    const [video_title, set_video_title] = useState("");
    const [video_description, set_video_description] = useState("");
    const [video_upload_at, set_video_upload_at] = useState("");

    useEffect(() => {
        const fetchVideo = async () => {
          const response = await fetch(`http://localhost:8092/streaming/video_file/${guid}`, {
            method: 'GET',
          });
    
          const blob = await response.blob();
          const blobUrl = URL.createObjectURL(blob);
          setVideoBlobUrl(blobUrl);
        };
    
        fetchVideo();
      }, [guid]);


    useEffect(() => {
        const fetchVideoInfo = async () => {
            try {
                const response = await streamingService.VideoFileInfo(guid);
                let data = response.data;
                console.log(data)

                set_channel_name(data.channel);
                set_video_title(data.title);
                set_video_description(data.description);
                set_is_processed(data.properlyProcessed);
                set_video_upload_at(data.trans_datetime);
            } catch (error) {
                console.error("Error fetching video info:", error);
            }
        };

        fetchVideoInfo();
    }, [guid]);

    useEffect(() => {
        const likeButton = document.getElementById("video_player_info_action_like");
        const dislikeButton = document.getElementById("video_player_info_action_dislike");

        const handleLikeClick = () => {
            if (isLiked === 0) {
                likeButton.style.backgroundColor = "rgb(255, 145, 0)";
                dislikeButton.style.backgroundColor = "rgb(210, 210, 210)";
                setIsLiked(1);
            } else {
                likeButton.style.backgroundColor = "rgb(210, 210, 210)";
                setIsLiked(0);
            }

            setIsDisliked(0);
        };

        const handleDislikeClick = () => {
            if (isDisliked === 0) {
                dislikeButton.style.backgroundColor = "rgb(255, 145, 0)";
                likeButton.style.backgroundColor = "rgb(210, 210, 210)";
                setIsDisliked(1);
            } else {
                dislikeButton.style.backgroundColor = "rgb(210, 210, 210)";
                setIsDisliked(0);
            }

            setIsLiked(0);
        };

        likeButton.addEventListener("click", handleLikeClick);
        dislikeButton.addEventListener("click", handleDislikeClick);

        return () => {
            likeButton.removeEventListener("click", handleLikeClick);
            dislikeButton.removeEventListener("click", handleDislikeClick);
        };
    }, [isLiked, isDisliked]);

    return (
        <>
            <div className="video_player_container">
                <div id="video_player">
                    {!is_processed && <span className="processing_error">Video is not yet processed.</span>}
                    <ReactPlayer 
                        url={videoBlobUrl} 
                        controls={true} 
                        width='100%' 
                        height='100%' 
                    />
                </div>

                <div className="video_player_info">
                    <div className="video_player_info_header">
                        <span className="video_player_info_title">{video_title}</span>

                        <div className="video_player_info_header_action">
                            <div className="video_player_info_channel_info">
                                <img src={profile}></img>
                                <span className="video_player_info_channel">{channel_name}</span>
                            </div>

                            <div className="video_player_info_action">
                                <div id="video_player_info_action_like">
                                    <img src={like}></img>
                                </div>
                                <div id="video_player_info_action_dislike">
                                    <img src={dislike}></img>
                                </div>
                                <div id="video_player_info_action_share">
                                    <img src={share}></img>
                                </div>
                            </div>
                        </div>
                    </div>

                    <span className="video_player_info_description">
                        {video_upload_at}

                        {video_description !== "" && <div>
                            <br></br>
                            {video_description}
                        </div>}
                    </span>
                </div>
            </div>

        </>
    );
}

export default WatchVideo;
