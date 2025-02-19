import React, { useState, useEffect } from 'react';
import './Upload.css';


import { UploadService } from '../../Service/UploadService';

function Upload() {
    const [file, setFile] = useState(null);
    const [video_pubblicity_status, set_video_pubblicity_status] = useState(0);
    const [progress, setProgress] = useState(0);
    const uploadService = new UploadService();


    function handleFileChange(event) {
        setFile(event.target.files[0]);
    }

    async function handleSubmit(event) {
        event.preventDefault();
        if (!file) {
            alert('Please select a file');
            return;
        }

        document.getElementById("video-upload-button").style.display = "none";

        const formData = new FormData();
        formData.append("video", file);

        try {
            const result = await uploadService.DoUploadVideo(formData, setProgress);

            if (result.data.status === 200) {
                setFile(null);
                alert("Video uploaded successfully!", result.message);
                console.log('Video uploaded successfully!:', result.status, result.message);
            } else {
                alert("Error uploading video!");
                console.error('Error uploading chunk:', result.status, result.message);
            }
        } catch (error) {
            console.error('Error uploading video:', error);
        }
    }


    function handleVideoStatusToggleSwitch() {
        const toggle = document.getElementById('video_status_toggle');

        toggle.classList.toggle('toggle-right');

        if (video_pubblicity_status === 0) {
            set_video_pubblicity_status(1);
        } else {
            set_video_pubblicity_status(0);
        }
    }


    return (
        <>
            <div className='container-upload '>
                <form onSubmit={handleSubmit} className='file-upload-form'>
                    <label className="drop-container" id="dropcontainer">
                        <span className="drop-title">Drop files here</span>
                        or
                        <input type="file" accept="video/*" onChange={handleFileChange} required />
                        <button type="submit" id='video-upload-button'>Upload Video</button>

                        {progress > 0 && (
                            <div className="upload_progress_bar">
                                <div className="progress_bar_container">
                                    <div className="upload_progress" style={{ width: `${progress}%` }}></div>
                                    <p className="progress_percentage">{progress}% Uploaded</p>
                                </div>
                            </div>

                        )}
                    </label>
                </form>


                <span>Title<span>*</span></span>
                <input type="text" className="upload_input upload_normal_input" />

                <span>Description<span>*</span></span>
                <textarea className="upload_input upload_textarea" rows="10"></textarea>

                <div className='thumbnail_and_save'>
                    <label className="drop-container-thumbnail" id="dropcontainer">
                        <span className="drop-title">Drop thumbnail here</span>
                        or
                        <input type="file" accept="image/*" onChange={handleFileChange} required />
                    </label>

                    <div className='video_right_side_buttons'>
                        <div className='video_publicity_switch_container'>
                            <span className='video_publicity_name'>Private</span>
                            <div className='video_publicity_switch' onClick={handleVideoStatusToggleSwitch}>
                                <div id='video_status_toggle'></div>
                            </div>
                            <span className='video_publicity_name'>Public</span>
                        </div>

                        <button className='video-save-button'>Save</button>
                    </div>
                </div>
            </div>
        </>
    );
}

export default Upload;