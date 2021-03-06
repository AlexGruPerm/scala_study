#include "stdafx.h"
#include <windows.h>
#include <iostream>
#include <conio.h>
#include "cassandra.h"
#include "DLLUse.h"

using namespace std;

int main()
{

	printf("Begin main()");

	if (OpenDLL())
	{
		cout << "Error: cannot open MATHLIBRARY.dll";
		_getch();
		return -1;
	}
	else {
		cout << "DLL MATHLIBRARY.dll is loaded";
	}

	printf("After OpenDLL, call savetick");

	int res = 0;

	//for (int i = 0; i < 10000; i++) {
	res = savetick("127.0.0.1", 1, 1.2334, 1.3445);
	res = savetick("127.0.0.1", 2, 2.2334, 2.3445);
	res = savetick("127.0.0.1", 3, 3.2334, 3.3445);
	res = savetick("127.0.0.1", 4, 3.2334, 3.3445);
//}

	printf("After savetick Before CloseDLL");

	_getch();
	CloseDLL();

	printf("After CloseDLL");
	return 0;
}

