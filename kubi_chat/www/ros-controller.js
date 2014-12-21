var ros_controller = function(opt) {
    this.ros = new ROSLIB.Ros({
        url: "ws://" + "127.0.0.1" + ":9090"
    });

    this.drive_topic = new ROSLIB.Topic({
        ros: this.ros,
        name: "/invented_pendulum/torque_offset_vector/once",
	// "/chat/request/drive_vector",
        messageType: 'std_msgs/Float32MultiArray'
    });

    this.string_topic = new ROSLIB.Topic({
        ros: this.ros,
        name: "/aria/commandline",
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
	    buf += "<input type=\"image\" src=\"" + command_list[i] + "\" name=\"" + command_list[i] + "\" value=\"forward\" onclick=\"rc.publish_string_command('" + topic_list[i] + "');\"></input>" ;
	}
    }
    elem.innerHTML=buf;
}

