export const HELP_SECTIONS: Record<string, { title: string; intro: string; faqs: { q: string; a: string }[] }> = {
  faq: {
    title: "Câu hỏi thường gặp",
    intro: "Tổng hợp các câu hỏi phổ biến từ khách hàng.",
    faqs: [
      { q: "Tôi cần đăng ký để mua hàng không?", a: "Bạn có thể mua hàng với tư cách khách. Tuy nhiên đăng ký tài khoản giúp theo dõi đơn hàng, lưu địa chỉ và nhận ưu đãi riêng." },
      { q: "Phương thức thanh toán nào được hỗ trợ?", a: "Hiện chúng tôi hỗ trợ Sepay (chuyển khoản QR), Ví MoMo và Thanh toán khi nhận hàng (COD)." },
      { q: "Đơn hàng được giao trong bao lâu?", a: "1-3 ngày đối với nội thành các thành phố lớn, 2-5 ngày đối với các tỉnh khác." },
      { q: "Tôi có thể đổi/trả hàng không?", a: "Bạn có thể đổi/trả trong vòng 30 ngày nếu sản phẩm còn nguyên tem mác, chưa qua sử dụng." },
      { q: "Voucher có dùng cùng nhau được không?", a: "Mỗi đơn hàng chỉ áp dụng được 1 voucher. Voucher freeship có thể dùng kèm với 1 voucher giảm giá sản phẩm." },
    ],
  },
  shipping: {
    title: "Chính sách vận chuyển",
    intro: "Mọi điều bạn cần biết về việc giao nhận.",
    faqs: [
      { q: "Phí ship bao nhiêu?", a: "30.000₫ trên toàn quốc. Miễn phí cho đơn hàng từ 500.000₫." },
      { q: "Thời gian giao hàng?", a: "1-3 ngày cho TP.HCM, Hà Nội, Đà Nẵng. 2-5 ngày các tỉnh khác. Hàng cồng kềnh có thể lâu hơn." },
      { q: "Đối tác vận chuyển?", a: "GHN, GHTK, Viettel Post và shipper nội bộ cho nội thành." },
      { q: "Tôi có thể chọn giờ giao không?", a: "Khi đặt hàng, bạn có thể ghi chú khung giờ mong muốn. Đối tác vận chuyển sẽ liên hệ trước khi giao." },
    ],
  },
  returns: {
    title: "Đổi trả & hoàn tiền",
    intro: "Quy trình đổi trả 30 ngày, không lăn tăn.",
    faqs: [
      { q: "Khi nào được đổi/trả?", a: "Trong 30 ngày kể từ ngày nhận hàng. Hàng còn nguyên tem mác, đầy đủ phụ kiện, hộp." },
      { q: "Quy trình thế nào?", a: "Vào trang đơn hàng → chọn 'Yêu cầu đổi/trả' → điền lý do → đợi xác nhận → gửi hàng về kho." },
      { q: "Bao lâu nhận hoàn tiền?", a: "3-7 ngày làm việc sau khi chúng tôi nhận và kiểm tra hàng. Hoàn về phương thức thanh toán ban đầu." },
      { q: "Có sản phẩm nào không được đổi trả?", a: "Hàng tiêu hao (mỹ phẩm đã mở, thực phẩm), hàng cá nhân hoá theo yêu cầu." },
    ],
  },
  payment: {
    title: "Thanh toán",
    intro: "Phương thức thanh toán an toàn, đa dạng.",
    faqs: [
      { q: "Sepay là gì?", a: "Sepay là cổng thanh toán cho phép bạn quét QR chuyển khoản qua mọi ngân hàng tại Việt Nam, không phí giao dịch." },
      { q: "COD có an toàn không?", a: "Có. Bạn chỉ trả tiền khi shipper giao đến tận tay và bạn kiểm tra hàng." },
      { q: "Đơn của tôi bị treo 'Chờ thanh toán'?", a: "Vui lòng vào trang đơn hàng và bấm 'Thanh toán ngay'. Nếu vẫn lỗi, liên hệ hotline 1900 1234." },
      { q: "Thông tin thẻ của tôi có an toàn không?", a: "Chúng tôi không lưu thông tin thẻ. Mọi giao dịch đi qua cổng đối tác có chứng nhận PCI-DSS." },
    ],
  },
};
