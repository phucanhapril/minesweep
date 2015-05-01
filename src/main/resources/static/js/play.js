UNEXPLORED = "#C0C0C0";
EXPLORED = "#FFFFFF";
NORMAL_BORDER = "#000000";
BOMB = "#000000";
BOMB_BORDER = "#FF0000";
TEXT_COLOR = "#000000";


var CANVAS_X = 800;
var CANVAS_Y = 800;

var tileWidth;
var tileHeight;

var globalBoard;

var _ctx;

$("#board").hide();

var server_ip = "" + location.host;
server_ip = server_ip.substring(0, server_ip.length - 5);

var socket = new WebSocket("ws://" + server_ip + ":8080");

socket.onopen = function(event) {
    $.getScript("../js/js.cookie.js", function(){
        var sendData = {
            type: "init",
            minesweepId: $.cookie("minesweepId"),
            minesweepRoomId: $.cookie("minesweepRoomId")
        };  
        socket.send(JSON.stringify(sendData));
    });
}

$("#startButton").click(function() {
    $.getScript("../js/js.cookie.js", function(){
        var sendData = {
            type: "startGame",
            minesweepId: $.cookie("minesweepId"),
            minesweepRoomId: $.cookie("minesweepRoomId")
        };  
        socket.send(JSON.stringify(sendData));
    });
});


socket.onmessage = function (event) {
    var responseJson = JSON.parse(event.data);
    
    // Pre game setup    
    if (responseJson.type === "update") {
        var innerBox = "";
        
        var roomInfo = responseJson.data;

        var teams = roomInfo.teams;

        $.each(teams, function(i, team) {
            innerBox += '<div class="span-3"><h4>' + team.name + "</h4>";
            $.each(team.players, function(j, player) {
                innerBox += '<a class="button line-purple">' + player.name + "</a><br>";
            });
            // add ai button
            innerBox += '<a class="button aqua modal-trigger" data-modal-open="ai-choose'+i+'" id="ai' + i + '">' + "Add AI</a><br>";
            // join team button
            innerBox += '<a class="button purple" id="buttonId' + i + '">' + "Join Team</a></div>";  
            // choose ai difficulty modal
            innerBox += '<div class="modalplate" data-modal-id="ai-choose'+i+'"><div class="modalplate-title-bar"><a class="close">Close</a><h4>Choose AI Difficulty</h4></div><div class="modalplate-content"><div class="row"><div class="span-2"><a class="button aqua large icon close" id="easy'+i+'"><span class="icon icon-smile"></span></a>Easy</div><div class="span-2"><a class="button aqua large icon close" id="medium'+i+'"><span class="icon icon-evil"></span></a>Medium</div><div class="span-2"><a class="button aqua large icon close" id="hard'+i+'"><span class="icon icon-crying"></span></a>Hard</div><div class="span-2"><a class="button aqua large icon close" id="random'+i+'"><span class="icon icon-hipster"></span></a>Random</div></div></div></div>';  
        });
        
         $("#teams").html(innerBox);
        
         $.each(teams, function(i, team) {
            $('#buttonId' + i).click(function(){
                console.log("human player added to team "+i);
                    $.getScript("../js/js.cookie.js", function(){
                        $.cookie("minesweepTeamId", i);
                        var sendData = {
                            type: "joinRoom",
                            minesweepId: $.cookie("minesweepId"),
                            minesweepTeamId: i,
                            minesweepRoomId: $.cookie("minesweepRoomId"),
                            name: "test name"
                    };
                    console.log(sendData);
                    socket.send(JSON.stringify(sendData));
                  });
            });
        });

        $.each(teams, function(i, team) {
            $('#easy' + i).click(function(){
                addAi(i, "EASY");
            });
            $('#medium' + i).click(function(){
                addAi(i, "MEDIUM");
            });
            $('#hard' + i).click(function(){
                addAi(i, "HARD");
            });
            $('#random' + i).click(function(){
                addAi(i, "MEDIUM");
            });
        });
    } 

    // Begin game, i.e. draw game board
    else if (responseJson.type === "gameData") {
        init();
        drawBoard(responseJson.data);
        console.log("Working");
        $("#board").show();
        $("#start").hide();
        $("#teams").hide();
    }
}

function addAi(teamId, difficulty) {
        $.getScript("../js/js.cookie.js", function(){
            $.cookie("minesweepTeamId", teamId);
            var sendData = {
                type: "addAIPlayer",
                minesweepId: $.cookie("minesweepId"),
                minesweepTeamId: $.cookie("minesweepTeamId"),
                minesweepRoomId: $.cookie("minesweepRoomId"),
                difficulty: difficulty
            };
            console.log(sendData);
            socket.send(JSON.stringify(sendData));
        });
}

function init() {
    var canvasBoard = $("#board")[0];
    canvasBoard.height = CANVAS_Y;
    canvasBoard.width = CANVAS_X;
    _ctx = canvasBoard.getContext("2d");
}

function drawBoard(responseJSON) {
    var board = JSON.parse(responseJSON);
    
    var width = board.width;
    var height = board.height;
    
    tileWidth = CANVAS_X / width;
    tileHeight = CANVAS_Y / height;
    
    var tiles = board.tiles;
    
    console.log(board.type);
    globalBoard = board;

    if (board.type == "DefaultBoard"){
        _ctx.clearRect(0, 0, CANVAS_X, CANVAS_Y);

        $.each(tiles, function(index, tile) {
            var tileX = tile.column * tileWidth;
            var tileY = tile.row * tileHeight;
            if (tile.visited) {
                if(tile.isBomb) {
                    _ctx.fillStyle = BOMB;
                    _ctx.fillRect(tileX, tileY, tileWidth, tileHeight);
                    _ctx.strokeStyle = BOMB_BORDER;
                    _ctx.strokeRect(tileX, tileY, tileWidth, tileHeight);
                } else {
                    _ctx.fillStyle = EXPLORED;
                    _ctx.fillRect(tileX, tileY, tileWidth, tileHeight);
                    _ctx.strokeStyle = NORMAL_BORDER;
                    _ctx.strokeRect(tileX, tileY, tileWidth, tileHeight);
                    _ctx.fillStyle = TEXT_COLOR;
                    _ctx.fillText(tile.adjacentBombs, tileX + 8, tileY + 12);                    
                }
            } else {
                _ctx.fillStyle = UNEXPLORED;
                _ctx.fillRect(tileX, tileY, tileWidth, tileHeight);
                _ctx.strokeStyle = NORMAL_BORDER;
                _ctx.strokeRect(tileX, tileY, tileWidth, tileHeight);
            }

        });
    } else if (board.type == "TriangularBoard"){

         _ctx.clearRect(0, 0, CANVAS_X, CANVAS_Y);
         tileWidth = CANVAS_X / (width / 2 + height / 2);

        $.each(tiles, function(index, tile) {
            var offset = tile.row * tileWidth / 2;
            if (tile.column % 2 === 0) {
                var x1 = tile.column / 2 * tileWidth + offset;
                var x2 = (tile.column / 2 + 1) * tileWidth + offset;
                var x3 = (tile.column / 2 + 0.5) * tileWidth + offset;
                var y1 = tile.row * tileHeight;
                var y2 = tile.row * tileHeight;
                var y3 = (tile.row + 1) * tileHeight;
            } else {
                var x1 = (tile.column / 2 + 0.5) * tileWidth + offset;
                var x2 = tile.column / 2 * tileWidth + offset;
                var x3 = (tile.column / 2 + 1) * tileWidth + offset;
                var y1 = tile.row * tileHeight;
                var y2 = (tile.row + 1) * tileHeight;
                var y3 = (tile.row + 1) * tileHeight;
            }
            triangleDraw(x1, x2, x3, y1, y2, y3, tile);
        });
   
        _ctx.stroke();
    } else {
        console.log("I had a stroke. Undefined board");
    }
}

function triangleDraw(x1, x2, x3, y1, y2, y3, tile) {
    _ctx.beginPath();
    _ctx.moveTo(x1, y1);
    _ctx.lineTo(x2, y2);
    //_ctx.moveTo(x2, y2);
    _ctx.lineTo(x3, y3);
    //_ctx.moveTo(x3, y3);
    _ctx.lineTo(x1, y1);
    _ctx.closePath();
    _ctx.strokeStyle = NORMAL_BORDER;
    _ctx.stroke();

    if (tile.visited) {
        if(tile.isBomb) {
            _ctx.fillStyle = BOMB;
            _ctx.fill();
            //_ctx.strokeStyle = BOMB_BORDER;
        } else {
            _ctx.fillStyle = EXPLORED;
            _ctx.fill();
            //_ctx.strokeStyle = NORMAL_BORDER;
            _ctx.fillStyle = TEXT_COLOR;
            _ctx.fillText(tile.adjacentBombs, (x1 + x2 + x3) / 3, (y1 + y2 + y3) / 3);                    
        }
    } else {
        _ctx.fillStyle = UNEXPLORED;
        _ctx.fill();
        //_ctx.strokeStyle = NORMAL_BORDER;
    }


    /*if (tile.isBomb) {
        fillColor = BOMB;
        _ctx.strokeStyle = BOMB_BORDER;
    } else if (tile.visited) {
        fillColor = EXPLORED;
        _ctx.strokeStyle = NORMAL_BORDER;
    } else {
        fillColor = UNEXPLORED;
        _ctx.strokeStyle = NORMAL_BORDER;
    }
    _ctx.fillStyle = fillColor;
    _ctx.beginPath();
    _ctx.moveTo(x1, y1);
    _ctx.lineTo(x2, y2);
    _ctx.moveTo(x2, y2);
    _ctx.lineTo(x3, y3);
    _ctx.moveTo(x3, y3);
    _ctx.lineTo(x1, y1);
    _ctx.fill();
    _ctx.fillStyle = TEXT_COLOR;
    _ctx.fillText(tile.adjacentBombs, (x1 + x2 + x3) / 3, (y1 + y2 + y3) / 3);  */
}


$("#board").bind('click', function(event){
    var board = $("#board")[0];

    console.log("click");

    var x = event.pageX - board.offsetLeft;
    var y = event.pageY - board.offsetTop;

    if (globalBoard.type == "DefaultBoard"){
        

        var row = Math.floor(y / tileHeight);
        var column = Math.floor(x / tileWidth);
    
        $.getScript("../js/js.cookie.js", function(){
            var sendData = {
                type: "makeMove",
                minesweepId: $.cookie("minesweepId"),
                minesweepRoomId: $.cookie("minesweepRoomId"),
                minesweepTeamId: $.cookie("minesweepTeamId"),
                row: row,
                col: column
            };
            socket.send(JSON.stringify(sendData));
        });

    } else if (globalBoard.type == "TriangularBoard") {
        var row = Math.floor(y / tileHeight);
        var offset = row * tileWidth / 2;
        var estimate = Math.floor((x - offset) / tileWidth * 2);
        console.log(row + " " + estimate);


        var tiles = globalBoard.tiles;
        var selectedTile;

        $.each(tiles, function(index, tile) {
            if (tile.row == row && tile.column == estimate) {
                console.log(tile.row + " " + tile.column);
                selectedTile = tile;
            }
            
        });

        if (selectedTile.column % 2 === 0) {
                var x1 = selectedTile.column / 2 * tileWidth + offset;
                var x2 = (selectedTile.column / 2 + 1) * tileWidth + offset;
                var x3 = (selectedTile.column / 2 + 0.5) * tileWidth + offset;
                var y1 = selectedTile.row * tileHeight;
                var y2 = selectedTile.row * tileHeight;
                var y3 = (selectedTile.row + 1) * tileHeight;
            } else {
                var x1 = selectedTile.column / 2 * tileWidth + offset;
                var x2 = (selectedTile.column / 2 + 1) * tileWidth + offset;
                var x3 = (selectedTile.column / 2 + 0.5) * tileWidth + offset;
                var y1 = (selectedTile.row + 1) * tileHeight;
                var y2 = (selectedTile.row + 1) * tileHeight;
                var y3 = selectedTile.row * tileHeight;
            }

        var borderSlope = (y3 - y1) / (x3 - x1);
        var clickSlope = (y - y1) / (x - x1);
        console.log(borderSlope + " " + clickSlope);
        if (Math.abs(borderSlope) < Math.abs(clickSlope)) {
            var column = estimate - 1;
            console.log("Above border");
        } else {
            var column = estimate;
            console.log("Below border");
        }
        console.log(row + " " + column);

        $.getScript("../js/js.cookie.js", function(){
            var sendData = {
                type: "makeMove",
                minesweepId: $.cookie("minesweepId"),
                minesweepRoomId: $.cookie("minesweepRoomId"),
                minesweepTeamId: $.cookie("minesweepTeamId"),
                row: row,
                col: column
            };
            socket.send(JSON.stringify(sendData));
        });
    }
});
