let EndpointMicroservice = {
    authentication: "http://localhost:8090",
    dashboard: "http://localhost:8091",
    streaming: "http://localhost:8092",
    upload: "http://localhost:8093",
}

let EndpointAuthentication = {
    do_signup: "/authentication/do_signup",
    do_login: "/authentication/do_login",
    get_userid_from_jwt: "/authentication/get_userid_from_jwt",
    logout: "/authentication/logout",
}

let EndpointDashboard = {
    menu: "/dashboard/menu",
}

let EndpointStreaming = {
    streaming: "/streaming/temp",
}

let EndpointUpload = {
    upload_video: "/upload/upload_video",
    upload_video_info: "/upload/upload_video_info",
    get_uploaded_video_list: "/manage_video/get_uploaded_video_list",
    edit_video: "/manage_video/edit_video",
    delete_video: "/manage_video/delete_video",
}

let EndpointWebsocket = {
    authentication_websocket: "ws://localhost:8090",
    get_websocket_emit: "/authentication-websocket",

    get_logout_emit: "/topic/logout",
    get_broadcast_emit: "/topic/broadcast",

    emit_data: "/app/send-message",
}

export { EndpointMicroservice, EndpointAuthentication, EndpointDashboard, EndpointStreaming, EndpointUpload, EndpointWebsocket };