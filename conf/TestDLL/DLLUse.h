#include <windows.h>

#define	DLL_LOAD_ERR	-1
#define DLL_FUNC_ERR	-2

/*
void	(*SetValue)	(double);
double	(*GetValue)	();
savetick(int id_symbol, double bid, double ask)
*/
int(*savetick) (const char*, int, double, double);

HMODULE	LibInst;

int OpenDLL()
{
	LibInst = LoadLibraryA("C:\\msvs\\MATHLIBRARY\\Debug\\MATHLIBRARY.dll");
	if (!LibInst)
		return DLL_LOAD_ERR;
	savetick = (int(*)(const char*,int, double, double)) GetProcAddress(LibInst, "savetick");
	/*
	SetValue	= (void(*)(double))	GetProcAddress (LibInst, "SetValue");
	GetValue	= (double(*)())		GetProcAddress (LibInst, "GetValue");
	*/
	return 0;
}

void CloseDLL()
{
	FreeLibrary(LibInst);
}
