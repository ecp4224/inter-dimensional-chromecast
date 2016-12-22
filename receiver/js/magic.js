//taken from https://github.com/mrmcpowned/interdimensionalcable/blob/gh-pages/js/get-video.js#L30
var youtube_video_regex = new RegExp(/(?:youtube\.com\/\S*(?:(?:\/e(?:mbed))?\/|watch\/?\?(?:\S*?&?v\=))|youtu\.be\/)([a-zA-Z0-9_-]{6,11})/);
var videos = [];
var played = [];
toastr.options = {
    "closeButton": false,
    "debug": false,
    "newestOnTop": false,
    "progressBar": false,
    "positionClass": "toast-top-center",
    "preventDuplicates": false,
    "onclick": null,
    "showDuration": "1800",
    "hideDuration": "1000",
    "timeOut": "1800",
    "extendedTimeOut": "1800",
    "showEasing": "swing",
    "hideEasing": "linear",
    "showMethod": "fadeIn",
    "hideMethod": "fadeOut"
}

if (!Array.prototype.randomElement) {
    Array.prototype.randomElement = function() {
        return this[Math.floor(Math.random() * this.length)];
    };
}

if (!Array.prototype.randomPop) {
    Array.prototype.randomPop = function() {
        var index = Math.floor(Math.random() * this.length);
        return this.splice(index, 1)[0];
    };
}

//taken from https://github.com/mrmcpowned/interdimensionalcable/blob/gh-pages/js/get-video.js#L38
//because it works..
var validateAndAdd = function(redditItem, array) {
    // Check if the URL is for youtube
    if (!youtube_video_regex.test(redditItem.url)) {
        return false;
    }
    // Check to see if the entier video is being linked.
    // If a certain index is being linked, ignore the video.
    if (redditItem.url.indexOf("t=") != -1) {
        return false;
    }
    // Check if a reddit post has less than 25 points.
    // If the post does, ignore it. It is unworthy.
    if (redditItem.score < 5) {
        return false;
    }
    var groups = youtube_video_regex.exec(redditItem.url);
    var video_id = groups[1];
    if (played.indexOf(video_id) != -1) {
        return false;
    }
    array.push(video_id);
    return true;
};

var getSearchUrl = function(time, sort) {
    return `https://www.reddit.com/r/InterdimensionalCable/search.json?q=site%3Ayoutube.com+OR+site%3Ayoutu.be&restrict_sr=on&sort=${sort}&t=${time}&limit=50`;
};

var getRandomSearchUrl = function() {
    var time = ["week", "month", "year", "all"].randomElement();
    var sort = ["relevance", "hot", "top", "new", "comments"].randomElement();
    var url = getSearchUrl(time, sort);

    return url;
};

var addRandomVideos = function(count, array) {
    var promise = new Promise(function(resolve, reject) {
        var search = getRandomSearchUrl();
        $.getJSON(search, function(response) {
                var count = 0;
                response.data.children.forEach(function(item) {
                    if (validateAndAdd(item.data, array))
                        count++;
                });
                if (count > 0)
                    resolve(count + " videos added");
                else {
                    reject("No videos added!");
                }
            })
            .error(function(err) {
                reject(err);
            });
    });

    return promise;
};

var getNextVideo = function() {
    if (videos.length == 0) {
        addRandomVideos(10, videos);
        return null;
    }
    return videos.randomPop();
}

var getNextVideoAsync = function() {
    return new Promise(function(resolve, reject) {
        if (videos.length == 0) {
            addRandomVideos(10, videos)
                .then(getNextVideoAsync)
                .then(function(id) {
                    resolve(id);
                })
                .error(function(err) {
                    reject(err);
                });
            return;
        }
        resolve(videos.randomPop());
    });
}

var player = null;
window.onYouTubePlayerAPIReady = function() {
    addRandomVideos(5, videos).then(function() {
        player = new YT.Player("player", {
            width: window.width,
            height: window.height,
            videoId: getNextVideo(),
            playerVars: {
                "autoplay": 1,
                "controls": 0,
                "showinfo": 0,
                "rel": 0,
                "iv_load_policy": 3,
                "disablekb": 1
            },
            events: {
                "onReady": onPlayerReady,
                "onStateChange": onPlayerStateChange,
                "onError": onPlayerError
            }
        });
    });
}

function onPlayerReady(event) {
    //event.target.playVideo();
}

function onPlayerStateChange(event) {
    //event.target.playVideo();
}

function onPlayerError(event) {

}

//Chromecast Code Begin
var gameManager;
var players = [];
var voteSkips = 0;

window.onload = function() {
    var castReceiverManager = cast.receiver.CastReceiverManager.getInstance();
    var appConfig = new cast.receiver.CastReceiverManager.Config();
    appConfig.statusText = 'Preparing Inter-Dimensional Cable...';
    appConfig.maxInactivity = 6000; // 100 minutes for testing only.

    var gameConfig = new cast.receiver.games.GameManagerConfig();
    gameConfig.applicationName = 'Inter-Dimensional Cable';
    gameConfig.maxPlayers = 100;
    gameManager = new cast.receiver.games.GameManager(gameConfig);

    castReceiverManager.start(appConfig);
    prepareEvents();
};

function prepareEvents() {
    gameManager.onPlayerAvailable = function(event) {
        players.push(event.playerInfo.playerId);
        toastr.info(event.playerInfo.playerId + " Joined");
    };

    gameManager.addEventListener(cast.receiver.games.EventType.GAME_MESSAGE_RECEIVED,
        function(event) {
            if (event.requestExtraMessageData == 'voteSkip') {
                voteSkips++;
                if (voteSkips >= players.length / 2) {
                    playNextVideo();
                } else {
                    toastr.info(event.playerInfo.playerId + " voted to skip!");
                }
            }
        });
}
