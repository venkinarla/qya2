#ifndef __COMMON_SPOTTER_H__
#define __COMMON_SPOTTER_H__

#define SIZEOF_SSID  33
#define SIZEOF_BSSID 18
#define MAX_APS 32

void throw_spotter_exception(char *message);
typedef void (*SawAPFunction)(char *bssid, char *ssid, int rss,int wep,int infrMode);

/* if the interfaceName is NULL or "" then simply choose first wireless interface */
void spotter_init(const char *interfaceName);

void spotter_shutdown();

/* returns 0 on success, -1 on failure */
int spotter_poll(SawAPFunction fn);


#endif /* __COMMON_SPOTTER_H__ */
