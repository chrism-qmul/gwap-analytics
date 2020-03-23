(ns dashboard.db)

(def initial {:loading 0
              :granularity :month
              :filters {
                    :game nil
                    :start-time nil
                    :end-time nil
                    :campaign #{}
                    :experiment #{}}
              :dimensions {}})
