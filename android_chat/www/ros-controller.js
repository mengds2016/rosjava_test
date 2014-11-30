var ros_controller = function(opt) {
    this.ros = new ROSLIB.Ros({
        url: "ws://" + "127.0.0.1" + ":9090"
    });

    this.drive_topic = new ROSLIB.Topic({
        ros: this.ros,
        name: "/chat/request/drive_vector",
        messageType: 'std_msgs/Float32MultiArray'
    });

    ros_controller.prototype.publish_drive_command = function(v,r){
        var msg = new ROSLIB.Message(
            {data: [v,r]}
        );
        this.drive_topic.publish(msg);
    };
};

var rc = new ros_controller() ;
