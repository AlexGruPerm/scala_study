package bar

case class BarFutureAnal(
                          ticker_id              :Int,
                          bar_width_sec          :Int,
                          ts_end                 :Long,
                          c                      :Double,
                          //----------------------------------
                          ft_log_0017_ts_end     :Long,
                          ft_log_0017_res        :String,//u,d
                          ft_log_0017_cls_price  :Double,
                          //----------------------------------
                          ft_log_0034_ts_end     :Long,
                          ft_log_0034_res        :String,//u,d
                          ft_log_0034_cls_price  :Double,
                          //----------------------------------
                          ft_log_0051_ts_end     :Long,
                          ft_log_0051_res        :String,//u,d
                          ft_log_0051_cls_price  :Double
                        )
