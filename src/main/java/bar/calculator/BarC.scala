package bar.calculator

case class 	BarC(
                 ticker_id       :Int,
                 ddate           :java.util.Date,
                 bar_width_sec   :Int,
                 ts_begin        :Long,
                 ts_end          :Long,
                 o               :Double,
                 h               :Double,
                 l               :Double,
                 c               :Double,
                 h_body          :Double,
                 h_shad          :Double,
                 btype           :String,
                 ticks_cnt       :Int,
                 disp            :Double,
                 log_co          :Double
                )

case class BarCPat(b :BarC,
                   //--------
                   ticks_cnt_prcnt      : Double,
                   ticks_cnt_prcnt_from : Double,
                   ticks_cnt_prcnt_to   : Double,
                   //--------
                   abs_logco_prcnt      : Double,
                   abs_logco_prcnt_from : Double,
                   abs_logco_prcnt_to   : Double)

case class BarFutAnalRes(
                          ticker_id              :Int,
                          bar_width_sec          :Int,
                          ts_end                 :Long,
                          c                      :Double,
                          ft_log_0017_ts_end     :Long,
                          ft_log_0017_res        :String,
                          ft_log_0017_cls_price  :Double,
                          ft_log_0034_ts_end     :Long,
                          ft_log_0034_res        :String,
                          ft_log_0034_cls_price  :Double,
                          ft_log_0051_ts_end     :Long,
                          ft_log_0051_res        :String,
                          ft_log_0051_cls_price  :Double
                        )