#include "stdafx.h"
#include <assert.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "cassandra.h"
#include <plog/Log.h>
#include <iostream> 


class Mt4Cass
{
private:
	static Mt4Cass* instance;

	Mt4Cass(const char* casshosts);
	~Mt4Cass();

public:
	static Mt4Cass* getInstance(const char* casshosts);
	CassSession*    session = NULL;
	CassCluster*    cluster = NULL;
	CassFuture*     prepared_future = NULL;
	CassStatement*  savetick_statement = NULL;

	const CassPrepared* savetick_prepared = NULL;
	const char*         savetick_query = "insert into mts_src.ticks(ticker_id, ddate, ts, bid, ask) values(?, TODATE(now()), toUnixTimestamp(now()), ?, ?);";
	//void on_result(CassFuture* future, void* data);
};

Mt4Cass* Mt4Cass::instance = 0;

Mt4Cass* Mt4Cass::getInstance(const char* casshosts)
{
	if (instance == 0)
	{
		instance = new Mt4Cass(casshosts);
	}
	return instance;
}


Mt4Cass::Mt4Cass(const char* casshosts) {
	session = cass_session_new();
	cluster = cass_cluster_new();
	cass_cluster_set_contact_points(cluster, casshosts);

	cass_cluster_set_num_threads_io(cluster, 4);
	cass_cluster_set_core_connections_per_host(cluster, 4); 

	CassFuture* connect_future = cass_session_connect(session, cluster);
	cass_future_wait(connect_future);

	prepared_future = cass_session_prepare(session, savetick_query);

	if (cass_future_error_code(prepared_future) != CASS_OK) {
		/* Handle error */
		const char* message;
		size_t message_length;
		cass_future_error_message(prepared_future, &message, &message_length);
		LOGE << message;
		cass_future_free(prepared_future);
	}
	else {
		savetick_prepared = cass_future_get_prepared(prepared_future);
		cass_future_free(prepared_future);
		savetick_statement = cass_prepared_bind(savetick_prepared);
	}

}

Mt4Cass::~Mt4Cass() {
	cass_cluster_free(cluster);
	cass_session_free(session);
	cass_future_free(prepared_future);
	cass_statement_free(savetick_statement);
}




BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved) {
//https://github.com/SergiusTheBest/plog

	plog::init(plog::error, "C:\\mt4logs\\mt4cass_log.txt");

	switch (fdwReason)
	{
	case DLL_PROCESS_ATTACH:
		LOGI << "DLL_PROCESS_ATTACH";
		break;

	case DLL_PROCESS_DETACH:
		LOGI << "DLL_PROCESS_DETACH";
		break;

	case DLL_THREAD_ATTACH:
		LOGI << "DLL_THREAD_ATTACH";
		break;

	case DLL_THREAD_DETACH:
		LOGI << "DLL_THREAD_DETACH";
		break;
	}

	return TRUE;
}


_DLLAPI int savetick(const char* casshosts, int id_symbol, double bid, double ask) {
	LOGI << "savetick begin";
	Mt4Cass* cass = Mt4Cass::getInstance(casshosts);

	cass_statement_bind_int32(cass->savetick_statement, 0, id_symbol);
	cass_statement_bind_double(cass->savetick_statement, 1, bid);
	cass_statement_bind_double(cass->savetick_statement, 2, ask);

	CassFuture* future = cass_session_execute(cass->session, cass->savetick_statement);
	cass_future_wait(future);

	if (cass_future_error_code(future) != CASS_OK) {
		/* Handle error */
		const char* message;
		size_t message_length;
		cass_future_error_message(future, &message, &message_length);
		cass_future_free(future);
		LOGE << message;
	}

	LOGI << "savetick end";
	return 0;
}