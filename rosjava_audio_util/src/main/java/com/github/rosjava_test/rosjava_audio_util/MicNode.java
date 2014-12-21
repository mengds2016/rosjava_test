/*
 * Copyright (C) 2014 noda.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava_test.rosjava_audio_util;

import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

public class MicNode extends AbstractNodeMain {

	private TargetDataLine targetDataLine;
	private MicTest mic;

	public static String node_name = "rosjava_test/mic";
	
	@Override
	public GraphName getDefaultNodeName() {
		String name = System.getenv("ROSJAVA_VOICE_MIC_DATA_NODE_NAME");
		if ( name != null ){
			node_name = name;
		}
		return GraphName.of(node_name);
	}

	@Override
	public void onShutdown(Node node){
		if ( this.targetDataLine != null ){
			this.targetDataLine.close();
		}
	}
	
	@Override
	public void onStart(final ConnectedNode connectedNode) {

		this.mic = new MicTest();
		try {
			this.targetDataLine = mic.setupTarget();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		node_name = connectedNode.getParameterTree().getString(
				"ROSJAVA_VOICE_MIC_DATA_NODE_NAME",
				"rosjava_test/mic"); 
		
		String mic_topic = connectedNode.getParameterTree().getString(
				"ROSJAVA_VOICE_MIC_DATA_TOPIC",
				node_name + "/data"); 
		
		final Publisher<audio_common_msgs.AudioData> publisher = connectedNode
				.newPublisher(mic_topic, audio_common_msgs.AudioData._TYPE);
		
		connectedNode.executeCancellableLoop(new CancellableLoop() {
			byte[] buffer = new byte[MicTest.BUFFER_SIZE];

			@Override
			protected void setup() {
			}
			@Override
			protected void loop() throws InterruptedException {
				MicNode.this.targetDataLine.read(this.buffer, 0, this.buffer.length);
				// mic.syncRead(this.buffer, 0, this.buffer.length);
				audio_common_msgs.AudioData data = publisher.newMessage();
				data.setData(ChannelBuffers.copiedBuffer(ByteOrder.LITTLE_ENDIAN, this.buffer, 0, this.buffer.length));
				publisher.publish(data);
			}
		});
	}
}
