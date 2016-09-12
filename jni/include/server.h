/*
 * server.h
 *
 *  Created on: 2016年9月5日
 *      Author: cbj
 */

#ifndef SERVER_H_
#define SERVER_H_
#include"stream_type.h"




#define SERVER_CMD_LOGIN 				0x00a0
#define SERVER_CMD_LOGOUT 			0x00a1
#define SERVER_CMD_START				0x00a2
#define SERVER_CMD_STOP					0x00a3

typedef int on_callback(int flag,void *data,int len);

int server_init();

int server_deInit();

int server_set_on_callback(on_callback* callback);

int server_start();

int send_data(int is_I,stream_head *head,const char *data,int len);

int server_stop();

int server_has_client_linked();



#endif /* SERVER_H_ */
