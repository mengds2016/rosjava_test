var ros_controller = function(opt) {
    this.ros = new ROSLIB.Ros({
        url: "ws://" + "127.0.0.1" + ":9090"
    });

    this.drive_topic = new ROSLIB.Topic({
        ros: this.ros,
        name: "/aria/color",
        messageType: 'std_msgs/Float32MultiArray'
    });

    this.string_topic = new ROSLIB.Topic({
        ros: this.ros,
        name: "/ros_chat/status/string",
        messageType: 'std_msgs/String'
    });

    ros_controller.prototype.publish_drive_command = function(v,r){
        var msg = new ROSLIB.Message(
            {data: [v,r]}
        );
	console.log("drive [" + v + "," + r + "]");
        this.drive_topic.publish(msg);
    };

    ros_controller.prototype.publish_string_command = function(str){
        var msg = new ROSLIB.Message(
            {data: str}
        );
	console.log("command line " + str);
        this.string_topic.publish(msg);
    };
};

var rc = new ros_controller() ;

function insert_container(tag, container, command_list, topic_list){
    var elem = document.getElementById(container);
    var i=0;
    var buf="";
    if ( tag == elem.title ){
	elem.title = "";
    } else {
	elem.title = tag;
	for ( ; i<command_list.length; i++ ){
	    buf += "<input style=\"margin:10px;\" type=\"image\" src=\"" + command_list[i] + "\" name=\"" + command_list[i] + "\" value=\"forward\" onclick=\"rc.publish_string_command('tag:" + topic_list[i] + "');\"></input>" ;
	}
    }
    elem.innerHTML=buf;
}

var prev_head_color="FFFFFF";
var prev_cheek_color="FFFFFF";
function publish_status_data(){
    var head = document.getElementById("head_color");
    if ( head == null ) return;
    if ( head.value != prev_head_color ){
	prev_head_color = head.value;
	var red = parseInt(head.value.substr(0,2),16);
	var grn = parseInt(head.value.substr(2,4),16);
	var blu = parseInt(head.value.substr(4,6),16);
	var msg = new ROSLIB.Message(
            {data: [0,3,red,grn,blu]}
	);
	rc.drive_topic.publish(msg);
    }
    //
    var cheek = document.getElementById("hope_color");
    if ( cheek.value != prev_cheek_color ){
	prev_cheek_color = cheek.value;
	var red = parseInt(cheek.value.substr(0,2),16);
	var grn = parseInt(cheek.value.substr(2,4),16);
	var blu = parseInt(cheek.value.substr(4,6),16);
	var msg = new ROSLIB.Message(
            {data: [3,5,red,grn,blu]}
	);
	rc.drive_topic.publish(msg);
    }
    //
    var toggle="toggle:";
    toggle += document.getElementById("neck_checkbox").checked ? "o" : "x";
    toggle += document.getElementById("eye_checkbox").checked ? "o" : "x";
    toggle += document.getElementById("mimi_checkbox").checked ? "o" : "x";
    toggle += document.getElementById("cup_checkbox").checked ? "o" : "x";
    rc.publish_string_command(toggle);
}

setInterval('publish_status_data()',500);
