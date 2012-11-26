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
package com.googlecode.protobuf.pro.stream.client;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibEncoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.jboss.netty.handler.ssl.SslHandler;

import com.googlecode.protobuf.pro.stream.RpcSSLContext;
import com.googlecode.protobuf.pro.stream.handler.Handler;
import com.googlecode.protobuf.pro.stream.wire.StreamProtocol;

public class StreamingTcpClientPipelineFactory implements
        ChannelPipelineFactory {

	private RpcSSLContext sslContext;
	private boolean compress;
	
    public StreamingTcpClientPipelineFactory() {
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = pipeline();
        
        if ( getSslContext() != null ) {
        	p.addLast(Handler.SSL, new SslHandler(getSslContext().createClientEngine()) );
        }
        
        // clients configure compression if the server has it configured.
        if ( isCompress() ) {
	    	p.addLast( Handler.DECOMPRESSOR, new ZlibEncoder(ZlibWrapper.GZIP));
	    	p.addLast( Handler.COMPRESSOR,  new ZlibDecoder(ZlibWrapper.GZIP));
        }
        
        p.addLast(Handler.FRAME_DECODER, new ProtobufVarint32FrameDecoder());
        p.addLast(Handler.PROTOBUF_DECODER, new ProtobufDecoder(StreamProtocol.WirePayload.getDefaultInstance()));

        p.addLast(Handler.FRAME_ENCODER, new ProtobufVarint32LengthFieldPrepender());
        p.addLast(Handler.PROTOBUF_ENCODER, new ProtobufEncoder());

        return p;
    }

	/**
	 * @return the sslContext
	 */
	public RpcSSLContext getSslContext() {
		return sslContext;
	}

	/**
	 * @param sslContext the sslContext to set
	 */
	public void setSslContext(RpcSSLContext sslContext) {
		this.sslContext = sslContext;
	}

	/**
	 * @return the compress
	 */
	public boolean isCompress() {
		return compress;
	}

	/**
	 * @param compress the compress to set
	 */
	public void setCompress(boolean compress) {
		this.compress = compress;
	}
    
}