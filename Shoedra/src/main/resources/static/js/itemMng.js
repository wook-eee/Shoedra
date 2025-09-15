//무한스크롤 변수
let page = 0;
const size = 10;
let isLoading = false;
let isLastPage = false;
let observer; // 선언만 먼저 해두기


const badgeMap = {
  READY: `<p class="auction-badge before">경매전</p>`,
  IN_PROGRESS: `<p class="auction-badge in-progress">경매중</p>`,
  ENDED: `<p class="auction-badge closed">경매마감</p>`
};

const badgeSellMap = {
    SELL: `<p class="auction-badge in-progress">판매중</p>`,
    SOLD_OUT: `<p class="auction-badge closed">품절</p>`
}



// 상품 카드 HTML 생성 함수
function createItemCard(item) {

    const stateLabel = badgeMap[item.auctionState] || '';
    const stateSellLabel = badgeSellMap[item.itemState] || '';

    return `
        <div class="product-card">
            <a href="/admin/item/${item.id}">
              <img src="${item.repImgUrl}" alt="상품 이미지" class="product-img" />
            </a>
            <div class="product-info">
              <h3 class="product-title">${item.itemNm}</h3>
              <p class="product-price">${item.price}원</p>
              ${stateLabel}
              ${stateSellLabel}
            </div>
            <div class="product-action">
              <button type="button" class="auction-btn"
                        data-item-id="${item.id}"
                        data-price="${item.price}"
                        data-title="${item.itemNm}" onclick="btnClick(event)">경매하기</button>
               <button type="button" class="sell-btn"
                                       data-item-id="${item.id}"
                                       data-price="${item.price}" onclick="btnClick(event)">판매하기</button>
            </div>
        </div>
       `;
} <!--//createItemCard() end -->

// 상품 목록 불러오기
function loadItems() {

    if (isLoading || isLastPage) return;

    isLoading = true;

    fetch(`/admin/items/load?page=${page}&size=${size}`)
        .then(res => res.json())
        .then(data => {
            console.log("▶ 페이지:", page, "| 마지막 페이지? ", data.content);

            const items = data.content;
            const grid = document.getElementById('productGrid');

            if (items.length === 0 && page === 0) {
                document.getElementById('productGrid').innerHTML = '<div style="text-align:center;padding:3rem;color:#666;">등록된 상품이 없습니다</div>';
                isLastPage = true;
                return;
            }

            items.forEach(item => {
                grid.insertAdjacentHTML('beforeend', createItemCard(item));
            });
            if (data.last) {
                isLastPage = true;
                if (observer) observer.disconnect(); // 감지 종료
            } else {
                page++;
            }
        })
        .finally(() => { isLoading = false; });
}

/*

    // 스크롤 이벤트
     window.addEventListener('scroll', function() {
         if (isLoading || isLastPage) return;
         const scrollY = window.scrollY || window.pageYOffset;
         const viewport = window.innerHeight;
         const fullHeight = document.body.offsetHeight;

         if (scrollY + viewport >= fullHeight - 100) {
               isLoading = true;
                loadItems(true);
         }
     });
*/

document.addEventListener('DOMContentLoaded', function() {
    loadItems();

    const sentinel = document.getElementById('sentinel');

    observer = new IntersectionObserver(entries => {
        if (entries[0].isIntersecting && !isLoading && !isLastPage) {
            loadItems();
        }
    });

    if (sentinel) {
        observer.observe(sentinel);
    } else {
        console.error("sentinel 요소가 없습니다.");
    }
});

function btnClick(e){
    let status;
    const itemId = e.target.getAttribute('data-item-id'); // 아이템번호
    const price = e.target.getAttribute('data-price');  //아이템가격

    if(e.target.matches('.auction-btn')){
        console.log("경매하기클릭");
        console.log("id:"+itemId);
        console.log("price:"+price);
        status = "";
        openAuctionModal(itemId, price, status);
    }else{ // .sell-btn
        console.log("판매하기클릭");
        console.log("id:"+itemId);
        console.log("price:"+price);
        status = "SELL";
        openAuctionModal(itemId, price, status);
    }

}



// modal창
/*
document.addEventListener('click', function (e) {
  // 1. 경매하기 버튼을 눌렀는지 확인
  if (e.target.matches('.auction-btn')) {
    const itemId = e.target.getAttribute('data-item-id'); // 아이템번호
    const price = e.target.getAttribute('data-price');  //아이템가격
    const title = e.target.getAttribute('data-title');  //아이템가격

    openAuctionModal(itemId, price, title);
  }

  // 2. 모달 닫기 버튼 (X) 눌렀는지 확인
  if (e.target.matches('.modal-close')) {
    closeAuctionModal();
  }

  // 3. 바깥 영역 클릭 시 닫기
  if (e.target.id === 'auctionModal') {
    closeAuctionModal();
  }
});
*/

function openAuctionModal(itemId, price, status) {
      const auctionModal = document.getElementById('auctionModal');
        const sellModal = document.getElementById('sellModal');
        console.log("상태:"+status);
      if(status === 'SELL'){
        sellModal.style.display = 'flex';
        document.getElementById('dataItemId').value = itemId;
        document.getElementById('price').value = price;
      }else{ //itemId
        auctionModal.style.display = 'flex';
        document.getElementById('itemId').value = itemId;
        document.getElementById('startPrice').value = price;
      }
  /*
  const modal = document.getElementById('auctionModal');
  modal.style.display = 'flex';

  document.getElementById('itemId').value = itemId;
  document.getElementById('startPrice').value = price;
  document.getElementById('auctionTitle').value = title;
  */

  // 가격 타입에러 발생시 변환필요
  //document.getElementById('startPrice').value = Number(price);
  // 아래 안전하게 Number로 변환
  //document.getElementById('startPrice').value = isNaN(numericPrice) ? '' : numericPrice;



  // 모달 안에 선택된 상품 ID 보여줄 수도 있음 (선택)
  // modal.querySelector('h2').textContent = `상품번호 ${itemId} 경매 등록`;
}

function closeAuctionModal() {
  const modal = document.getElementById('auctionModal');
  const sellModal = document.getElementById('sellModal');

  modal.style.display = 'none';
  sellModal.style.display = 'none';
}