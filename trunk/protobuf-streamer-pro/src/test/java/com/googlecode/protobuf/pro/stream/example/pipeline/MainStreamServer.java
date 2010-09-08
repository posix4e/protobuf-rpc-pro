/**
 *   Copyright 2010 Peter Klauser
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
*/
package com.googlecode.protobuf.pro.stream.example.pipeline;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.DefaultChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.googlecode.protobuf.pro.stream.CleanShutdownHandler;
import com.googlecode.protobuf.pro.stream.PeerInfo;
import com.googlecode.protobuf.pro.stream.TransferOut;
import com.googlecode.protobuf.pro.stream.client.StreamingTcpClientBootstrap;
import com.googlecode.protobuf.pro.stream.example.pipeline.Pipeline.Get;
import com.googlecode.protobuf.pro.stream.example.pipeline.Pipeline.Post;
import com.googlecode.protobuf.pro.stream.server.PullHandler;
import com.googlecode.protobuf.pro.stream.server.PushHandler;
import com.googlecode.protobuf.pro.stream.server.StreamingServerBootstrap;
import com.googlecode.protobuf.pro.stream.util.FileTransferUtils;

public class MainStreamServer {

	private static Log log = LogFactory.getLog(MainStreamServer.class);

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err
					.println("usage: <serverHostname> <serverPort>");
			System.exit(-1);
		}
		String serverHostname = args[0];
		int serverPort = Integer.parseInt(args[1]);

		PeerInfo serverInfo = new PeerInfo(serverHostname, serverPort);

		PullHandler<Get> pullHandler = new PullHandler<Get>() {

			@Override
			public Get getPrototype() {
				return Get.getDefaultInstance();
			}

			@Override
			public void handlePull(Get message, TransferOut transferOut) {
				// The client wants to pull the Get.file back
				// the thread calling this is from a pool
				log.info("Pull " + message);
				String filename = message.getFilename();
				
				File file = new File(filename);
				try {
					FileTransferUtils.sendFile(file, transferOut, true);

					log.info("Sent " + filename);
				} catch (IOException e) {
					log.warn("Pull failed ", e);
				}
			}

		};

		StreamingTcpClientBootstrap<Get,Post> clientBootstrap = new StreamingTcpClientBootstrap<Get,Post>(
				serverInfo, new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool())
				);
		clientBootstrap.setOption("sendBufferSize", 1048576);
		clientBootstrap.setOption("receiveBufferSize", 1048576);
		
		// we need to switch off this check, because we will be using IO-Threads from the server
		// to send and await transfer for writes to the clientBootstrap.
		DefaultChannelFuture.setUseDeadLockChecker(false);
		
		PushHandler<Post> pushHandler = new PipelinePushHandler(serverInfo, clientBootstrap);
		
		// Configure the server.
		StreamingServerBootstrap<Get, Post> bootstrap = new StreamingServerBootstrap<Get, Post>(
				serverInfo, pullHandler, pushHandler,
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
        bootstrap.setOption("sendBufferSize", 1048576);
        bootstrap.setOption("receiveBufferSize", 1048576);
        bootstrap.setOption("child.receiveBufferSize", 1048576);
        bootstrap.setOption("child.sendBufferSize", 1048576);

		// give the bootstrap to the shutdown handler so it is shutdown cleanly.
		CleanShutdownHandler shutdownHandler = new CleanShutdownHandler();
		shutdownHandler.addResource(bootstrap);

		// Bind and start to accept incoming connections.
		bootstrap.bind();

		log.info("Handling " + serverInfo);
	}

}
