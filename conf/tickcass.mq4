#property version   "1.0"
#import "kernel32.dll"

int GetModuleHandleA(string lpString);
int FreeLibrary(int hModule);
int LoadLibraryA(string lpString);

#import "MATHLIBRARY.dll"

int savetick(char&[],char&[], double pBid, double pAsk, int currdt);

      /*
      if (cSymbol == "EURUSD") return 1;
      if (cSymbol == "AUDUSD") return 2;
      if (cSymbol == "GBPUSD") return 3;
      if (cSymbol == "NZDUSD") return 4;
      if (cSymbol == "EURCHF") return 5;
      if (cSymbol == "USDCAD") return 6;
      if (cSymbol == "USDCHF") return 7;
      if (cSymbol == "EURCAD") return 8;
      if (cSymbol == "GBPAUD") return 9;
      if (cSymbol == "GBPCAD") return 10;
      if (cSymbol == "GBPCHF") return 11;
      if (cSymbol == "EURGBP") return 12;
      if (cSymbol == "GBPNZD") return 13;
      if (cSymbol == "NZDCAD") return 14;
      */

char hosts[10240];
char cSymbol[10240];

int OnInit(){
   StringToCharArray("127.0.0.1", hosts);
   char cSymbol[10240];
   return(INIT_SUCCEEDED);
}

void start(){
}

void OnTick(){
 StringToCharArray(Symbol(), cSymbol);
 int res=savetick(hosts,cSymbol,Bid,Ask,TimeCurrent());
}

