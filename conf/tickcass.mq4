//+------------------------------------------------------------------+
//|                                                     tickcass.mq4 |
//|                        Copyright 2018, MetaQuotes Software Corp. |
//|                                             https://www.mql5.com |
//+------------------------------------------------------------------+
#property version   "1.0"
#import "kernel32.dll"

   int GetModuleHandleA(string lpString);
   int FreeLibrary(int hModule);
   int LoadLibraryA(string lpString);

#import "MATHLIBRARY.dll"

int savetick(char&[],int pIdSymbol, double pBid, double pAsk, int currdt);

int symbolId;
char hosts[10240];

int OnInit()
  {
      if (Symbol() == "EURUSD") symbolId=1;
      if (Symbol() == "AUDUSD") symbolId=2;
      if (Symbol() == "GBPUSD") symbolId=3;
      if (Symbol() == "NZDUSD") symbolId=4;
      if (Symbol() == "EURCHF") symbolId=5;
      if (Symbol() == "USDCAD") symbolId=6;
      if (Symbol() == "USDCHF") symbolId=7;
      if (Symbol() == "EURCAD") symbolId=8;
      if (Symbol() == "GBPAUD") symbolId=9;
      if (Symbol() == "GBPCAD") symbolId=10;
      if (Symbol() == "GBPCHF") symbolId=11;
      if (Symbol() == "EURGBP") symbolId=12;
      if (Symbol() == "GBPNZD") symbolId=13;
      if (Symbol() == "NZDCAD") symbolId=14;

   StringToCharArray("127.0.0.1", hosts);
   return(INIT_SUCCEEDED);
  }

void start(){
}

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
  {
   int res=savetick(hosts,symbolId,Bid,Ask,TimeCurrent());
  }
//+------------------------------------------------------------------+
