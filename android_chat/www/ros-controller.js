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

    ros_controller.prototype.publish_drive_command = function(v,r){
        var msg = new ROSLIB.Message(
            {data: [v,r]}
        );
	console.log("drive [" + v + "," + r + "]");
        this.drive_topic.publish(msg);
    };

    ros_controller.prototype.publish_string_command = function(str){
    };
};

var rc = new ros_controller() ;

function insert_container(tag, command_list){
    var elem = document.getElementById("test_container");
    var i=0;
    var buf="";
    if ( tag == elem.title ){
	elem.title = "";
    } else {
	elem.title = tag;
	for ( ; i<command_list.length; i++ ){
	    buf += "<input type=\"image\" src=\"./test.jpg\" name=\"" + command_list[i] + "\" value=\"forward\" onclick=\"rc.publish_string_command(" + command_list[i] + ");\"></input>" ;
	}
    }
    elem.innerHTML=buf;
}

